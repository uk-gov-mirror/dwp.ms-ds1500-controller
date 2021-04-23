package uk.gov.dwp.health.ds1500controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.components.drs.DrsPayloadBuilder;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.crypto.exceptions.EventsMessageException;
import uk.gov.dwp.health.ds1500controller.application.Ds1500ControllerConfiguration;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.Ds1500Metadata;
import uk.gov.dwp.health.ds1500controller.domain.Views;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidJsonException;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.PdfRetrievalError;
import uk.gov.dwp.health.ds1500controller.utils.PdfRetriever;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.messageq.exceptions.EventsManagerException;
import uk.gov.dwp.health.messageq.items.event.EventMessage;
import uk.gov.dwp.health.messageq.items.event.MetaData;
import uk.gov.dwp.regex.InvalidNinoException;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

@Path("/")
public class Ds1500ControllerResource {
  private static final Logger LOG = LoggerFactory.getLogger(Ds1500ControllerResource.class);
  private static final String ERROR_MSG = "Unable to process request";
  private Ds1500ControllerConfiguration controllerConfiguration;
  private MetadataBuilder metadataBuilder;
  private Ds1500JsonValidator validator;
  private MessagePublisher snsPublish;
  private PdfRetriever pdfRetriever;
  private PdfRetriever feePdfRetriever;

  @Inject
  public Ds1500ControllerResource(
      Ds1500ControllerConfiguration config,
      MessagePublisher snsPublish,
      PdfRetriever pdfRetriever,
      PdfRetriever feePdfRetriever,
      Ds1500JsonValidator validator,
      MetadataBuilder metadataBuilder) {
    this.feePdfRetriever = feePdfRetriever;
    this.snsPublish = snsPublish;
    this.metadataBuilder = metadataBuilder;
    this.controllerConfiguration = config;
    this.pdfRetriever = pdfRetriever;
    this.validator = validator;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("controller")
  public Response post(String jsonPayload) {
    ObjectMapper mapper = new ObjectMapper();
    Response response;
    DSForm form;
    try {
      form = validator.validateAndTranslate(jsonPayload);
      LOG.info("Submission received from {}", form.getDeclarerName());

      Ds1500Metadata drsMetadata = metadataBuilder.buildPayload(form, LocalDate.now());

      UUID correlationId = UUID.randomUUID();

      MetaData metaData =
          new MetaData(Collections.singletonList(controllerConfiguration.getSnsSubject()));
      metaData.setRoutingKey(controllerConfiguration.getSnsRoutingKey());
      metaData.setCorrelationId(correlationId.toString());

      EventMessage messageQueueEvent = new EventMessage();
      messageQueueEvent.setBodyContents(
          mapper.readValue(
              new DrsPayloadBuilder<DSForm, Ds1500Metadata>().getDrsPayloadJson(form, drsMetadata),
              Object.class));
      messageQueueEvent.setMetaData(metaData);

      publishMessageToSns(messageQueueEvent);

      LOG.info(
          "DS1500 form successfully published to SNS from {} with correlationId {}",
          form.getDeclarerName(),
          correlationId);
      response =
          Response.ok(
                  String.format("{\"id\":\"%s\"}", correlationId.toString()),
                  MediaType.APPLICATION_JSON_TYPE)
              .build();

    } catch (JsonProcessingException | InvalidJsonException | InvalidNinoException e) {
      response =
          Response.status(Response.Status.BAD_REQUEST)
              .entity("JSON payload failed validation")
              .build();
      LOG.error("JSON validation failed :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);

    } catch (EventsManagerException e) {
      response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
      LOG.error("Publishing events manager exception :: {}", e.getMessage());
      LOG.debug(e.getClass().getName(), e);
    }

    return response;
  }

  @POST
  @Path("download")
  public Response download(@FormParam("json") String jsonPayload) {
    LOG.info("DS1500 PDF document download requested");
    return downloadPdfDocument(
        jsonPayload,
        controllerConfiguration.getPdfGeneratorUrl(),
        controllerConfiguration.getDs1500DownloadPdfName(),
        DSForm.class,
        pdfRetriever);
  }

  @POST
  @Path("downloadFee")
  public Response downloadFee(@FormParam("json") String jsonPayload) {
    Response response;
    try {
      DSForm dsForm = validator.validateAndTranslate(jsonPayload);
      if ("General Practitioner".equals(dsForm.getDeclaration())
          || "GMC registered consultant".equals(dsForm.getDeclaration())) {
        LOG.info("DS1500 Fee PDF document download requested");
        response =
            downloadPdfDocument(
                jsonPayload,
                controllerConfiguration.getPdfFeeGeneratorUrl(),
                controllerConfiguration.getDs1500FeeDownladPdfName(),
                Views.DsFeeForm.class,
                feePdfRetriever);
      } else {
        LOG.info("DS1500 fee document is not required, rejecting download attempt");
        response =
            Response.status(Response.Status.BAD_REQUEST)
                .entity("DS1500 fee document is not applicable for this submission")
                .build();
      }

    } catch (InvalidJsonException e) {
      LOG.error("Invalid json characters passed through to pdf fee download");
      LOG.debug(e.getClass().getName(), e);
      response =
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid json characters passed into pdf download")
              .build();
    }

    return response;
  }

  private void publishMessageToSns(EventMessage messageQueueEvent) throws EventsManagerException {
    try {

      LOG.debug(
          "Publish message to sns topic exchange '{}' with routing key '{}'",
          controllerConfiguration.getSnsTopicName(),
          controllerConfiguration.getSnsRoutingKey());
      snsPublish.publishMessageToSnsTopic(
          controllerConfiguration.isSnsEncryptMessages(),
          controllerConfiguration.getSnsTopicName(),
          controllerConfiguration.getSnsSubject(),
          messageQueueEvent,
          null);

    } catch (InstantiationException
        | InvocationTargetException
        | NoSuchMethodException
        | JsonProcessingException
        | IllegalAccessException
        | EventsMessageException
        | CryptoException e) {
      throw new EventsManagerException(e.getMessage());
    }
  }

  private Response downloadPdfDocument(
      String jsonPayload,
      String pdfGeneratorURL,
      String pdfResultFilename,
      Class<?> viewClass,
      PdfRetriever pdfDocType) {
    LOG.debug("download '{}' calling '{}'", pdfResultFilename, pdfGeneratorURL);
    Response response;
    DSForm dsForm;
    try {
      dsForm = validator.validateAndTranslate(jsonPayload);

      byte[] bytes =
          pdfDocType.getPdf(convertFormIntoJsonString(dsForm, viewClass), pdfGeneratorURL);
      Response.ResponseBuilder ok = Response.ok(bytes, "application/download");
      ok.header(
          "Content-Disposition", String.format("attachment; filename=\"%s\"", pdfResultFilename));

      LOG.info(
          "PDF file generated ({}) from {} and offered for download",
          pdfResultFilename,
          pdfGeneratorURL);
      response = ok.build();

    } catch (PdfRetrievalError e) {
      LOG.error("PDF generation failed for download from {}", pdfGeneratorURL);
      LOG.debug(e.getClass().getName(), e);
      response = Response.serverError().build();
    } catch (InvalidJsonException | IOException e) {
      LOG.error("Invalid json characters passed through to pdf download from {}", pdfGeneratorURL);
      LOG.debug(e.getClass().getName(), e);
      response =
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid json characters passed into pdf download")
              .build();
    }

    return response;
  }

  private String  convertFormIntoJsonString(DSForm form, Class<?> classView)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    return mapper.writerWithView(classView).writeValueAsString(form);
  }
}
