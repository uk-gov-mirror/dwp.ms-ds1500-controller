package uk.gov.dwp.health.ds1500controller.integration;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.CryptoMessage;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.messageq.EventConstants;
import uk.gov.dwp.health.messageq.amazon.items.AmazonConfigBase;
import uk.gov.dwp.health.messageq.amazon.items.messages.SnsMessageClassItem;
import uk.gov.dwp.health.messageq.amazon.utils.AmazonQueueUtilities;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"squid:S2925", "squid:S1192"}) // string literals allowed and Thread.sleep
public class ControllerSteps {
    private static final String FULL_JSON_REQUEST_GP_CONSULTANT = "{\n" +
            "  \"patientName\":\"Paul Coupe\",\n" +
            "  \"patientAddress\":\"23 Seabrook Road\\nNorfolk Park\\nNorfolk\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"patientNino\":\"NY921108A\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-day\":\"11\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            "  \"otherDiagnoses\":\"Mild allergies to dub step\",\n" +
            "  \"patientAware\":\"Yes\",\n" +
            "  \"formRequester\":\"Representative\",\n" +
            "  \"representativeName\":\"Representative name\",\n" +
            "  \"representativeAddress\":\"21 Representative Road\",\n" +
            "  \"representativePostcode\":\"NW1 3ER\",\n" +
            "  \"clinicalFeatures\":\"Mr Coupe's clinical features\",\n" +
            "  \"treatment\":\"Beer, loud music, fluffy kittens\",\n" +
            "  \"otherIntervention\":\"None whatsoever\",\n" +
            "  \"declaration\":\"General Practitioner\",\n" +
            "  \"declarationAdditionalDetail\":\"\",\n" +
            "  \"gpName\":\"Dr. Michael Hfuhruhurr\",\n" +
            "  \"gpAddress\":\"Porter Brook Medical Centre\",\n" +
            "  \"gpPhone\":\"0114 258 8520\",\n" +
            "  \"gmcNumber\":\"1234567\"}";

    private static final String FULL_JSON_REQUEST_OTHER = "{\n" +
            "  \"patientName\":\"Paul Coupe\",\n" +
            "  \"patientAddress\":\"23 Seabrook Road\\nNorfolk Park\\nNorfolk\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"patientNino\":\"NY921108A\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-day\":\"11\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            "  \"otherDiagnoses\":\"Mild allergies to dub step\",\n" +
            "  \"patientAware\":\"Yes\",\n" +
            "  \"formRequester\":\"Representative\",\n" +
            "  \"representativeName\":\"Representative name\",\n" +
            "  \"representativeAddress\":\"21 Representative Road\",\n" +
            "  \"representativePostcode\":\"NW1 3ER\",\n" +
            "  \"clinicalFeatures\":\"Mr Coupe's clinical features\",\n" +
            "  \"treatment\":\"Beer, loud music, fluffy kittens\",\n" +
            "  \"otherIntervention\":\"None whatsoever\",\n" +
            "  \"declaration\":\"Other\",\n" +
            "  \"declarationAdditionalDetail\":\"Nurse\",\n" +
            "  \"gpName\":\"McMillan Nurse\",\n" +
            "  \"gpAddress\":\"Porter Brook Medical Centre\",\n" +
            "  \"gpPhone\":\"0114 258 8520\"}";

    private static final String FULL_JSON_REQUEST_BAD_GP = "{\n" +
            "  \"patientName\":\"Paul Coupe\",\n" +
            "  \"patientAddress\":\"23 Seabrook Road\\nNorfolk Park\\nNorfolk\",\n" +
            "  \"patientPostcode\":\"S2 2RZ\",\n" +
            "  \"patientDateOfBirth-day\":\"15\",\n" +
            "  \"patientDateOfBirth-month\":\"02\",\n" +
            "  \"patientDateOfBirth-year\":\"1972\",\n" +
            "  \"patientNino\":\"NY921108A\",\n" +
            "  \"diagnosis\":\"Extreme grumpiness\",\n" +
            "  \"dateOfDiagnosis-day\":\"11\",\n" +
            "  \"dateOfDiagnosis-month\":\"05\",\n" +
            "  \"dateOfDiagnosis-year\":\"2015\",\n" +
            "  \"otherDiagnoses\":\"Mild allergies to dub step\",\n" +
            "  \"patientAware\":\"Yes\",\n" +
            "  \"formRequester\":\"Representative\",\n" +
            "  \"representativeName\":\"Representative name\",\n" +
            "  \"representativeAddress\":\"21 Representative Road\",\n" +
            "  \"representativePostcode\":\"NW1 3ER\",\n" +
            "  \"clinicalFeatures\":\"Mr Coupe's clinical features\",\n" +
            "  \"treatment\":\"Beer, loud music, fluffy kittens\",\n" +
            "  \"otherIntervention\":\"None whatsoever\",\n" +
            "  \"declaration\":\"General Practitioner\",\n" +
            "  \"declarationAdditionalDetail\":\"\",\n" +
            "  \"gpName\":\"Dr. Michael Hfuhruhurr\",\n" +
            "  \"gpAddress\":\"Porter Brook Medical Centre\",\n" +
            "  \"gpPhone\":\"0114 258 8520\"}";

    private static final String INVALID_JSON = "{\"messy\":\"Lionel\"";

    private final byte[] pdfDownloadResponse = Base64.getEncoder().encode("dummy pdf bytes".getBytes());
    private final byte[] pdfResponse = "dummy pdf bytes".getBytes();

    private final ObjectMapper mapper = new ObjectMapper();
    private AmazonQueueUtilities queueUtilities;
    private CryptoDataManager awsKmsCryptoClass;
    private List<Message> queueMessages;
    private HttpResponse response;
    private String jsonResponse;
    private HttpClient httpClient;

    @Rule
    private final WireMockServer pdfGenerator = new WireMockRule(wireMockConfig().port(9015));

    @Rule
    private final WireMockServer pdfFeeGenerator = new WireMockRule(wireMockConfig().port(9990));

    @Before
    public void startServer() throws CryptoException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, IOException {

        // create local properties to negate KMS from needing to access Metadata Service for IAM role privs
        System.setProperty("aws.accessKeyId", "this_is_my_system_property_key");
        System.setProperty("aws.secretKey", "abcd123456789");

        AmazonConfigBase snsConfig = new AmazonConfigBase();
        snsConfig.setEndpointOverride("http://localstack:4566");
        snsConfig.setS3EndpointOverride("http://localstack:4566");
        snsConfig.setLargePayloadSupportEnabled(false);
        snsConfig.setPathStyleAccessEnabled(true);
        snsConfig.setS3BucketName("sns-bucket");
        snsConfig.setRegion(Regions.US_EAST_1);

        AmazonConfigBase sqsConfig = new AmazonConfigBase();
        sqsConfig.setEndpointOverride("http://localstack:4566");
        sqsConfig.setS3EndpointOverride("http://localstack:4566");
        sqsConfig.setLargePayloadSupportEnabled(false);
        sqsConfig.setPathStyleAccessEnabled(true);
        sqsConfig.setS3BucketName("sqs-bucket");
        sqsConfig.setRegion(Regions.US_EAST_1);

        queueUtilities = new AmazonQueueUtilities(sqsConfig, snsConfig);

        CryptoConfig cryptoConfig = new CryptoConfig("test_request_id");
        cryptoConfig.setKmsEndpointOverride("http://localstack:4566");
        awsKmsCryptoClass = new CryptoDataManager(cryptoConfig);

        pdfGenerator.start();
        pdfFeeGenerator.start();
    }

    @After
    public void stopServer() {
        pdfGenerator.stop();
        pdfFeeGenerator.stop();
    }

    @Given("^the httpClient is up$")
    public void theControllerIsUp() {
        httpClient = HttpClientBuilder.create().build();
    }


    @When("^I hit the service url \"([^\"]*)\" with a body of \"([^\"]*)\"$")
    public void iHitTheServiceUrlWithAUrlParameterOf(String serviceUrl, String body) throws IOException {
        performHttpPostWithUriOf(serviceUrl, body);
    }

    @And("^the response contains the following JSON entries$")
    public void theResponseContainsTheFollowingJsonEntries(DataTable entries) throws IOException {
        Map<String, String> table = entries.asMap(String.class, String.class);
        JsonNode jsonNode = mapper.readTree(jsonResponse);
        for (Map.Entry<String, String> item : table.entrySet()) {
            JsonNode idp = jsonNode.findValue(item.getKey());
            assertThat(idp.textValue(), is(item.getValue()));
        }
    }

    @And("^the response content type is \"([^\"]*)\"$")
    public void theResponseContentTypeIs(String contentType) {
        response.getFirstHeader("Content-Type").getValue();
        assertEquals(contentType, response.getFirstHeader("Content-Type").getValue());
    }

    @Then("^I get a http response of (\\d+)$")
    public void iGetAHttpResponseOf(int statusCode) {
        assertThat(response.getStatusLine().getStatusCode(), is(statusCode));
    }

    @Then("^the response has a header of \"([^\"]*)\"$")
    public void theResponseHasAHeaderOf(String headerName) {
        assertNotNull(response.getFirstHeader(headerName));
    }

    @And("^the response has the message \"([^\"]*)\"$")
    public void theResponseHasTheMessage(String message) {
        assertThat(jsonResponse, is(message));
    }

    @And("^the response contains no Json$")
    public void theResponseContainsNoJson() throws IOException {
        boolean containsJson = true;
        if ((null != jsonResponse) && (!jsonResponse.isEmpty())) {
            try {
                new ObjectMapper().readTree(jsonResponse);

            } catch (JsonParseException e) {
                containsJson = false;
            }
        } else {
            containsJson = false;
        }
        assertFalse(containsJson);
    }

    @And("^the PDF Server will return a response of (\\d+) and a PDF file$")
    public void thePDFServerWillReturnAResponseOfAndAPDFFile(int responseCode) {
        pdfGenerator.stubFor(post(urlEqualTo("/")).withRequestBody(containing("surname")).willReturn(aResponse().withBody(pdfResponse).withStatus(responseCode)));
    }

    @And("^the PDF download service will return a response of (\\d+) and a PDF file$")
    public void thePdfDownloadServiceWillReturnAResponseOfAndAPdfFile(int responseCode) {
        pdfGenerator.stubFor(post(urlEqualTo("/")).willReturn(aResponse().withBody(pdfDownloadResponse).withStatus(responseCode)));
    }

    @And("^the PDF FEE Server will return a response of (\\d+) and a PDF file$")
    public void thePdfFeeServerWillReturnAResponseOfAndAPdfFile(int responseCode) {
        pdfFeeGenerator.stubFor(post(urlEqualTo("/")).withRequestBody(containing("surname")).willReturn(aResponse().withBody(pdfResponse).withStatus(responseCode)));
    }

    @And("^the PDF FEE download service will return a response of (\\d+) and a PDF file$")
    public void thePdfFeeDownloadServiceWillReturnAResponseOfAndAPdfFile(int responseCode) {
        pdfFeeGenerator.stubFor(post(urlEqualTo("/")).willReturn(aResponse().withBody(pdfDownloadResponse).withStatus(responseCode)));
    }

    @And("^the response contains a PDF file$")
    public void theResponseContainsAPdfFile() {
        HttpEntity responseEntity = response.getEntity();
        Header contentType = responseEntity.getContentType();
        assertThat(contentType.getValue(), is("application/download"));
        assertTrue("length of file should be greater than 0", responseEntity.getContentLength() > 0);
    }

    @And("^the PDF download service is unavailable$")
    public void thePDFDownloadServiceIsUnavailable() {
        pdfGenerator.stop();
    }

    @And("^the PDF Server is unavailable$")
    public void thePDFServerIsUnavailable() {
        pdfFeeGenerator.stop();

    }

    @When("^I hit the service url \"([^\"]*)\" with an invalid body$")
    public void iHitTheServiceUrlWithAnInvalidBody(String serviceUrl) throws IOException {
        performHttpPostWithUriOf(serviceUrl, INVALID_JSON);
    }

    @When("^I hit the service url \"([^\"]*)\" with a json form post gp consultant json body$")
    public void iHitTheServiceUrlWithAFormPostGPConsultantJsonBody(String serviceUrl) throws IOException {
        performHttpFormPostWithUriOf(serviceUrl, FULL_JSON_REQUEST_GP_CONSULTANT);
    }

    @When("^I hit the service url \"([^\"]*)\" with a json form post 'other' declarer json body$")
    public void iHitTheServiceUrlWithAFormPostOtherJsonBody(String serviceUrl) throws IOException {
        performHttpFormPostWithUriOf(serviceUrl, FULL_JSON_REQUEST_OTHER);
    }

    @When("^I hit the service url \"([^\"]*)\" with a post request gp consultant json body$")
    public void iHitTheServiceUrlWithAPostRequestGPConsultantJsonBody(String serviceUrl) throws IOException {
        performHttpPostWithUriOf(serviceUrl, FULL_JSON_REQUEST_GP_CONSULTANT);
    }

    @When("^I hit the service url \"([^\"]*)\" with a post request 'other' declarer json body$")
    public void iHitTheServiceUrlWithAPostRequestOtherJsonBody(String serviceUrl) throws IOException {
        performHttpPostWithUriOf(serviceUrl, FULL_JSON_REQUEST_OTHER);
    }

    @When("^I hit the service url \"([^\"]*)\" with a mismatched post declaration json body$")
    public void iHitTheServiceUrlWithAMismatchDeclarationJsonBody(String serviceUrl) throws IOException {
        performHttpPostWithUriOf(serviceUrl, FULL_JSON_REQUEST_BAD_GP);
    }

    @When("^I hit the service url \"([^\"]*)\"$")
    public void iHitTheServiceUrl(String url) throws IOException {
        performHttpGetWithUriOf(url);
    }

    @And("^I create an sns topic named \"([^\"]*)\"$")
    public void iCreateAnSnsTopicNamed(String topicName) {
        queueUtilities.createTopic(topicName);
    }

    @And("^I create a catch all subscription for queue name \"([^\"]*)\" binding to topic \"([^\"]*)\"$")
    public void iCreateACatchAllSubscriptionForQueueNameBindingToExchange(String queueName, String topicName) {
        queueUtilities.createQueue(queueName);
        queueUtilities.purgeQueue(queueName);
        queueUtilities.subscribeQueueToTopic(queueName, topicName);
    }

    @And("^a message is successfully removed from the queue, there were a total of (\\d+) messages on queue \"([^\"]*)\"$")
    public void thereIsPendingMessageOnQueue(int totalMessages, String queueName) throws IOException {
        queueMessages = queueUtilities.receiveMessages(queueName, queueUtilities.getS3Sqs());

        assertThat("mismatched messages", queueMessages.size(), is(equalTo(totalMessages)));

        assertNotNull("queue contents are null", queueMessages);
        queueUtilities.deleteMessageFromQueue(queueName, queueMessages.get(0).getReceiptHandle());
    }

    @And("^the message has a correlation id and is a valid DSForm matching the following submission information$")
    public void theMessageIsTakenFromTheQueueHasACorrelationIdAndAValidDsFormMatchingTheSubmissionInformation(Map<String, String> jsonValues) throws Throwable {
        assertNotNull(queueMessages);
        assertTrue(queueMessages.size() > 0);

        SnsMessageClassItem snsMessageClass = new SnsMessageClassItem().buildMessageClassItem(queueMessages.get(0).getBody());
        String msgContents = snsMessageClass.getMessage();

        if (snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER) != null) {
            CryptoMessage cryptoMessage = new CryptoMessage();
            cryptoMessage.setKey(snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER).getStringValue());
            cryptoMessage.setMessage(msgContents);

            msgContents = awsKmsCryptoClass.decrypt(cryptoMessage);

            assertThat(snsMessageClass.getMessageAttributes().get(EventConstants.KMS_DATA_KEY_MARKER).getDataType(), is(equalTo(EventConstants.AWS_MSG_ATTR_STRING)));
        }

        JsonNode payload = new ObjectMapper().readTree(msgContents).path("payload");

        for (Map.Entry<String, String> field : jsonValues.entrySet()) {
            assertThat(payload.path(field.getKey()).toString(), is(equalTo(field.getValue())));
        }
    }

    @And("^I wait (\\d+) seconds to guarantee message delivery$")
    public void iWait(int seconds) throws InterruptedException {
        TimeUnit.SECONDS.sleep(seconds);
    }

    /**
     * private methods
     */

    private void performHttpPostWithUriOf(String uri, String body) throws IOException {
        HttpPost httpUriRequest = new HttpPost(uri);
        HttpEntity entity = new StringEntity(body);
        httpUriRequest.setEntity(entity);
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
    }

    private void performHttpFormPostWithUriOf(String uri, String body) throws IOException {
        HttpPost httpUriRequest = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new BasicNameValuePair("json", body));
        httpUriRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        response = httpClient.execute(httpUriRequest);
        HttpEntity responseEntity = response.getEntity();
        jsonResponse = EntityUtils.toString(responseEntity);
    }

    private void performHttpGetWithUriOf(String uri) throws IOException {
        HttpUriRequest httpUriRequest = new HttpGet(uri);
        response = httpClient.execute(httpUriRequest);
        HttpEntity entity = response.getEntity();
        jsonResponse = EntityUtils.toString(entity);
    }
}
