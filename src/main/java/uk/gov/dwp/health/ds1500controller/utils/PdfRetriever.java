package uk.gov.dwp.health.ds1500controller.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.gov.dwp.health.ds1500controller.domain.exceptions.PdfRetrievalError;
import uk.gov.dwp.tls.TLSConnectionBuilder;
import uk.gov.dwp.tls.TLSGeneralException;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class PdfRetriever {

  private TLSConnectionBuilder sslAuthenticatedConnection;

  public PdfRetriever(TLSConnectionBuilder sslConnection) {
    this.sslAuthenticatedConnection = sslConnection;
  }

  public byte[] getPdf(String translatedJson, String pdfGeneratorUrl) throws PdfRetrievalError {
    byte[] bytes;

    HttpPost post = new HttpPost(pdfGeneratorUrl);
    try {
      CloseableHttpClient httpsClient = sslAuthenticatedConnection.configureSSLConnection();
      post.setEntity(new StringEntity(translatedJson));
      HttpResponse httpResponse = httpsClient.execute(post);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      if (statusCode == 200) {
        try (InputStream content = httpResponse.getEntity().getContent()) {
          bytes = IOUtils.toByteArray(content);
        }
      } else {
        throw new PdfRetrievalError(
            String.format(
                "Status code of %d returned when calling PDF Generator on %s",
                statusCode, pdfGeneratorUrl));
      }
    } catch (IOException
        | CertificateException
        | NoSuchAlgorithmException
        | UnrecoverableKeyException
        | TLSGeneralException
        | KeyStoreException
        | KeyManagementException e) {
      throw new PdfRetrievalError(e);
    }

    return bytes;
  }
}
