package uk.gov.dwp.health.ds1500controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.crypto.exceptions.EventsMessageException;
import uk.gov.dwp.health.ds1500controller.application.Ds1500ControllerConfiguration;
import uk.gov.dwp.health.ds1500controller.domain.DSForm;
import uk.gov.dwp.health.ds1500controller.domain.Ds1500Metadata;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.InvalidJsonException;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.PdfRetrievalError;
import uk.gov.dwp.health.ds1500controller.utils.PdfRetriever;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.messageq.items.event.EventMessage;
import uk.gov.dwp.regex.InvalidNinoException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S1192") // string literals allowed
public class Ds1500ControllerResourceTest {

    private static final String MAIN_JSON_WITH_DOCTORS_NAME = "{\"surname\":null,\"otherNames\":null,\"dateOfBirth\":null,\"nINumber\":null,\"address\":null,\"postcode\":null,\"diagnosis\":null,\"otherDiagnosis\":null,\"patientAware\":null,\"formRequestor\":null,\"diagnosisDate\":null,\"representative1\":null,\"representative2\":null,\"representative3\":null,\"clinicalFeatures\":null,\"treatment\":null,\"otherTreatment\":null,\"declaration\":\"GP\",\"other\":null,\"declarerName\":\"Dr Smith\",\"declarerPhoneNumber\":null,\"declarerAddress\":null,\"declarationDate\":null,\"gmcNumber\":0}";
    private static final String FEE_JSON_WITH_DOCTORS_NAME = "{\"surname\":null,\"otherNames\":null,\"dateOfBirth\":null,\"nINumber\":null,\"address\":null,\"postcode\":null,\"declarerName\":\"Dr Smith\",\"declarerAddress\":null,\"declarationDate\":null,\"gmcNumber\":0}";
    private static final String DS1500_PDF_FEE_URL = "http://localhost:9090";
    private static final String DS1500_PDF_URL = "http://localhost:9012";
    private static final String DS1500_FEE_PDF = "generatedDS1500Fee.pdf";
    private static final String DS1500_PDF = "generatedDS1500.pdf";
    private static final String MSG_TOPIC = "test.exchange";
    private static final String ROUTING_KEY = "routing.key";
    private static final String MSG_SUBJECT = "DS1500";

    @Mock
    private Ds1500ControllerConfiguration configuration;

    @Mock
    private Ds1500JsonValidator validator;

    @Mock
    private MessagePublisher snsPublish;

    @Mock
    private PdfRetriever retriever;

    @Mock
    private MetadataBuilder metadataBuilder;

    @InjectMocks
    private Ds1500ControllerResource resourceUnderTest;

    @Before
    public void setup() {
        when(configuration.getDs1500FeeDownladPdfName()).thenReturn(DS1500_FEE_PDF);
        when(configuration.getDs1500DownloadPdfName()).thenReturn(DS1500_PDF);

        when(configuration.getSnsRoutingKey()).thenReturn(ROUTING_KEY);
        when(configuration.getSnsTopicName()).thenReturn(MSG_TOPIC);
        when(configuration.getSnsSubject()).thenReturn(MSG_SUBJECT);
        when(configuration.isSnsEncryptMessages()).thenReturn(true);

        when(configuration.getPdfFeeGeneratorUrl()).thenReturn(DS1500_PDF_FEE_URL);
        when(configuration.getPdfGeneratorUrl()).thenReturn(DS1500_PDF_URL);
    }

    @Test
    public void confirmJsonIsValidated() throws InvalidJsonException, InvalidNinoException, JsonProcessingException, InvocationTargetException, EventsMessageException, InstantiationException, NoSuchMethodException, IllegalAccessException, CryptoException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        Ds1500Metadata metadata = new Ds1500Metadata();

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(metadataBuilder.buildPayload(any(DSForm.class), any(LocalDate.class))).thenReturn(metadata);

        Response post = resourceUnderTest.post(jsonPayload);
        assertThat(post.getStatus(), is(200));

        verify(validator).validateAndTranslate(jsonPayload);
        verify(snsPublish).publishMessageToSnsTopic(eq(true), eq(MSG_TOPIC), eq(MSG_SUBJECT), any(EventMessage.class), any(Map.class));
    }

    @Test
    public void confirmJsonPayloadIsSentToDrsCommunicator() throws InvalidJsonException, InvalidNinoException, IOException, NoSuchMethodException, InvocationTargetException, EventsMessageException, InstantiationException, IllegalAccessException, CryptoException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        Ds1500Metadata metadata = new Ds1500Metadata();

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(metadataBuilder.buildPayload(any(DSForm.class), any(LocalDate.class))).thenReturn(metadata);

        Response post = resourceUnderTest.post(jsonPayload);
        assertThat(post.getStatus(), is(200));

        JsonNode jsonNode = new ObjectMapper().readTree(post.getEntity().toString());
        UUID.fromString(jsonNode.get("id").textValue());

        verify(snsPublish).publishMessageToSnsTopic(eq(true), eq(MSG_TOPIC), eq(MSG_SUBJECT), any(EventMessage.class), any(Map.class));
    }

    @Test
    public void confirmDownloadReturnsAPdf() throws PdfRetrievalError, InvalidJsonException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("GP");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        byte[] bytes = "dummy pdf".getBytes();

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(retriever.getPdf(json.capture(), url.capture())).thenReturn(bytes);

        Response post = resourceUnderTest.download(jsonPayload);
        assertThat(post.getHeaderString("Content-Disposition"), is(String.format("attachment; filename=\"%s\"", DS1500_PDF)));
        assertThat(json.getValue(), is(MAIN_JSON_WITH_DOCTORS_NAME));
        assertThat(post.getEntity(), is(bytes));
        assertThat(url.getValue(), is(DS1500_PDF_URL));
        assertThat(post.getStatus(), is(200));

        verifyZeroInteractions(snsPublish);
    }


    @Test
    public void downloadPdfRetrievalErrorThrownAnd500Returned() throws PdfRetrievalError, InvalidJsonException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("GP");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(retriever.getPdf(json.capture(), url.capture())).thenThrow(new PdfRetrievalError("ERROR In pdf retrieval for test"));

        Response post = resourceUnderTest.download(jsonPayload);

        assertEquals(500, post.getStatus());
        assertEquals("Internal Server Error", post.getStatusInfo().getReasonPhrase());

    }

    @Test
    public void downloadInvalidJsonErrorThrownAnd500Returned() throws InvalidJsonException {
        String jsonPayload = "{}";

        when(validator.validateAndTranslate(jsonPayload)).thenThrow(new InvalidJsonException("Invalid JSON in test"));

        Response post = resourceUnderTest.download(jsonPayload);

        assertEquals(400, post.getStatus());
        assertEquals("Bad Request", post.getStatusInfo().getReasonPhrase());
        assertEquals("Invalid json characters passed into pdf download", post.getEntity());
    }


    @Test
    public void confirmDownloadFeeReturnsAPdfWhenDeclarationIsGeneralPractitioner() throws PdfRetrievalError, InvalidJsonException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("General Practitioner");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        byte[] bytes = "dummy pdf".getBytes();

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(retriever.getPdf(json.capture(), url.capture())).thenReturn(bytes);

        Response post = resourceUnderTest.downloadFee(jsonPayload);
        assertThat(post.getHeaderString("Content-Disposition"), is(String.format("attachment; filename=\"%s\"", DS1500_FEE_PDF)));
        assertThat(json.getValue(), is(FEE_JSON_WITH_DOCTORS_NAME));
        assertThat(post.getEntity(), is((bytes)));
        assertThat(url.getValue(), is(DS1500_PDF_FEE_URL));
        assertThat(post.getStatus(), is(200));

        verifyZeroInteractions(snsPublish);
    }


    @Test
    public void confirmDownloadFeeThrowsInvalidJsonErrorReturns400() throws PdfRetrievalError, InvalidJsonException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("General Practitioner");

        when(validator.validateAndTranslate(jsonPayload)).thenThrow(new InvalidJsonException("Error thrown for test"));

        Response post = resourceUnderTest.downloadFee(jsonPayload);

        assertEquals(400, post.getStatus());
        assertEquals("Bad Request", post.getStatusInfo().getReasonPhrase());
        assertEquals("Invalid json characters passed into pdf download", post.getEntity());

    }

    @Test
    public void confirmDownloadFeeReturnsAPdfWhenDeclarationIsGMCRegisteredConsultant() throws PdfRetrievalError, InvalidJsonException {
        String jsonPayload = "{}";
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("GMC registered consultant");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        byte[] bytes = ("dummy pdf".getBytes());

        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);
        when(retriever.getPdf(json.capture(), url.capture())).thenReturn(bytes);

        Response post = resourceUnderTest.downloadFee(jsonPayload);
        assertThat(post.getHeaderString("Content-Disposition"), is(String.format("attachment; filename=\"%s\"", DS1500_FEE_PDF)));
        assertThat(json.getValue(), is(FEE_JSON_WITH_DOCTORS_NAME));
        assertThat(post.getEntity(), is((bytes)));
        assertThat(url.getValue(), is(DS1500_PDF_FEE_URL));
        assertThat(post.getStatus(), is(200));

        verifyZeroInteractions(snsPublish);
    }

    @Test
    public void confirmDownloadFeeDoesntReturnAPdfWhenDeclarationIsOther() throws InvalidJsonException {
        DSForm returnedForm = new DSForm();
        returnedForm.setDeclarerName("Dr Smith");
        returnedForm.setDeclaration("Other");

        when(validator.validateAndTranslate(anyString())).thenReturn(returnedForm);

        Response post = resourceUnderTest.downloadFee("{}");
        assertNull("Other shouldn't trigger fee form download", post.getHeaderString("Content-Disposition"));
        assertThat("Message body should be DS1500 fee document is not applicable for this submission", post.getEntity(), is(equalTo("DS1500 fee document is not applicable for this submission")));
        assertThat("Should be bad request", post.getStatus(), is(400));
    }

    @Test
    public void confirmServiceReturnsServerErrorWhenDrsCommunicatorFails() throws JsonProcessingException, InvocationTargetException, EventsMessageException, InstantiationException, NoSuchMethodException, IllegalAccessException, CryptoException, InvalidNinoException, InvalidJsonException {
        Ds1500Metadata metadata = new Ds1500Metadata();
        DSForm returnedForm = new DSForm();
        String jsonPayload = "{}";

        returnedForm.setDeclarerName("Dr Smith");
        metadata.setBenefitType(99);

        doThrow(new EventsMessageException("Thrown for test purposes")).when(snsPublish).publishMessageToSnsTopic(eq(true), eq(MSG_TOPIC), eq(MSG_SUBJECT), any(EventMessage.class), any(Map.class));
        when(metadataBuilder.buildPayload(any(DSForm.class), any(LocalDate.class))).thenReturn(metadata);
        when(validator.validateAndTranslate(jsonPayload)).thenReturn(returnedForm);

        Response post = resourceUnderTest.post(jsonPayload);
        assertThat(post.getStatus(), is(500));
        verify(snsPublish).publishMessageToSnsTopic(eq(true), eq(MSG_TOPIC), eq(MSG_SUBJECT), any(EventMessage.class), any(Map.class));
    }

    @Test
    public void confirmJsonWhichIsNotValidatedWillNotSendToDrs() throws InvalidJsonException {
        String jsonPayload = "{badJSON}";
        when(validator.validateAndTranslate(jsonPayload)).thenThrow(new InvalidJsonException("thrown in test"));
        Response post = resourceUnderTest.post(jsonPayload);
        assertThat(post.getStatus(), is(400));
        verifyZeroInteractions(snsPublish);
    }
}