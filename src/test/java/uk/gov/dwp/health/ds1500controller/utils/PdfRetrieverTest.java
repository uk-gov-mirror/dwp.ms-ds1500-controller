package uk.gov.dwp.health.ds1500controller.utils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.dwp.health.ds1500controller.application.Ds1500ControllerConfiguration;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.PdfRetrievalError;
import uk.gov.dwp.tls.TLSConnectionBuilder;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PdfRetrieverTest {
    public static final int PORT = 9988;
    public static final String FULL_URL = "https://localhost:" + PORT + "/";

    public static final String FEE_PDF_TRUST_STORE_PATH = "src/test/resources/__files/ControllerFeePdfTruststore.ts";
    public static final String FEE_PDF_KEY_STORE_PATH = "src/test/resources/__files/FeePdfGenerator.ks";
    public static final String FEE_PDF_TRUST_AND_KEY_STORE_PASS = "password";

    @Rule
    public WireMockRule pdfGeneratorServer = new WireMockRule(wireMockConfig().port(1234)
            .httpsPort(PORT).needClientAuth(true).trustStorePath(FEE_PDF_TRUST_STORE_PATH)
            .trustStorePassword(FEE_PDF_TRUST_AND_KEY_STORE_PASS).keystorePath(FEE_PDF_KEY_STORE_PATH)
            .keystorePassword(FEE_PDF_TRUST_AND_KEY_STORE_PASS));

    @Mock
    private Ds1500ControllerConfiguration ds1500ControllerConfiguration;
    private TLSConnectionBuilder connectionBuilder;

    @Before
    public void setup() throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {

        when(ds1500ControllerConfiguration.getSslTruststoreFilenameFeePdf()).thenReturn(FEE_PDF_TRUST_STORE_PATH);
        when(ds1500ControllerConfiguration.getSslTruststorePasswordFeePdf()).thenReturn(FEE_PDF_TRUST_AND_KEY_STORE_PASS);
        when(ds1500ControllerConfiguration.getSslKeystoreFilenameFeePdf()).thenReturn(FEE_PDF_KEY_STORE_PATH);
        when(ds1500ControllerConfiguration.getSslKeystorePasswordFeePdf()).thenReturn(FEE_PDF_TRUST_AND_KEY_STORE_PASS);

        connectionBuilder = new TLSConnectionBuilder(ds1500ControllerConfiguration.getSslTruststoreFilenameFeePdf(),
                ds1500ControllerConfiguration.getSslTruststorePasswordFeePdf(),
                ds1500ControllerConfiguration.getSslKeystoreFilenameFeePdf(),
                ds1500ControllerConfiguration.getSslKeystorePasswordFeePdf());

        pdfGeneratorServer.start();
    }

    @After
    public void stopServer() {
        pdfGeneratorServer.stop();
    }

    @Test
    public void confirmPdfGeneratorServiceIsCalledAndReturnsPdf() throws PdfRetrievalError {
        PdfRetriever retriever = new PdfRetriever(connectionBuilder);

        String translatedPayload = "{}";
        byte[] bytes = "test".getBytes();
        pdfGeneratorServer.stubFor(post(urlEqualTo("/")).withRequestBody(equalTo(translatedPayload)).willReturn(aResponse().withBody(bytes).withStatus(200)));
        byte[] pdf = retriever.getPdf(translatedPayload, FULL_URL);
        assertThat(pdf, is(bytes));
    }

    @Test(expected = PdfRetrievalError.class)
    public void whenPdfGeneratorCallFailsThenExceptionIsThrown() throws PdfRetrievalError {
        PdfRetriever retriever = new PdfRetriever(connectionBuilder);

        pdfGeneratorServer.stubFor(post(urlEqualTo("/")).withRequestBody(equalTo("{}")).willReturn(aResponse().withStatus(500)));
        retriever.getPdf("{}", FULL_URL);
    }
}