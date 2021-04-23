package uk.gov.dwp.health.ds1500controller.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import uk.gov.dwp.crypto.SecureStrings;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.messageq.amazon.items.AmazonConfigBase;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("java:S2637") // NotNull constraint - class is populated by serialisation
public class Ds1500ControllerConfiguration extends Configuration {
  private SecureStrings cipher = new SecureStrings();

  @NotNull
  @JsonProperty("snsTopicName")
  private String snsTopicName;

  @NotNull
  @JsonProperty("snsRoutingKey")
  private String snsRoutingKey;

  @NotNull
  @JsonProperty("snsSubject")
  private String snsSubject;

  @JsonProperty("snsEncryptMessages")
  private boolean snsEncryptMessages = true;

  @NotNull
  @JsonProperty("snsConfiguration")
  private AmazonConfigBase snsConfiguration;

  @JsonProperty("kmsCryptoConfiguration")
  private CryptoConfig kmsCryptoConfig;

  @NotNull
  @JsonProperty("ds1500FeeDownladPdfName")
  private String ds1500FeeDownladPdfName;

  @NotNull
  @JsonProperty("ds1500DownloadPdfName")
  private String ds1500DownloadPdfName;

  @NotNull
  @JsonProperty("pdfFeeGeneratorUrl")
  private String pdfFeeGeneratorUrl;

  @NotNull
  @JsonProperty("pdfGeneratorUrl")
  private String pdfGeneratorUrl;

  @JsonProperty("sslTruststoreFilenamePdf")
  private String sslTruststoreFilenamePdf;

  @JsonProperty("sslTruststorePasswordPdf")
  private SealedObject sslTruststorePasswordPdf;

  @JsonProperty("sslKeystoreFilenamePdf")
  private String sslKeystoreFilenamePdf;

  @JsonProperty("sslKeystorePasswordPdf")
  private SealedObject sslKeystorePasswordPdf;

  @JsonProperty("sslTruststoreFilenameFeePdf")
  private String sslTruststoreFilenameFeePdf;

  @JsonProperty("sslTruststorePasswordFeePdf")
  private SealedObject sslTruststorePasswordFeePdf;

  @JsonProperty("sslKeystoreFilenameFeePdf")
  private String sslKeystoreFilenameFeePdf;

  @JsonProperty("sslKeystorePasswordFeePdf")
  private SealedObject sslKeystorePasswordFeePdf;

  @JsonProperty("applicationInfoEnabled")
  private boolean applicationInfoEnabled;

  public Ds1500ControllerConfiguration()
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    // required to support 1.5.3+ of secure-strings
  }

  public boolean isApplicationInfoEnabled() {
    return applicationInfoEnabled;
  }

  public CryptoConfig getKmsCryptoConfig() {
    return kmsCryptoConfig;
  }

  public String getDs1500FeeDownladPdfName() {
    return ds1500FeeDownladPdfName;
  }

  public String getDs1500DownloadPdfName() {
    return ds1500DownloadPdfName;
  }

  public String getPdfFeeGeneratorUrl() {
    return pdfFeeGeneratorUrl;
  }

  public String getPdfGeneratorUrl() {
    return pdfGeneratorUrl;
  }

  public String getSslTruststoreFilenamePdf() {
    return sslTruststoreFilenamePdf;
  }

  public String getSslTruststorePasswordPdf() {
    return cipher.revealString(sslTruststorePasswordPdf);
  }

  public String getSslKeystoreFilenamePdf() {
    return sslKeystoreFilenamePdf;
  }

  public String getSslKeystorePasswordPdf() {
    return cipher.revealString(sslKeystorePasswordPdf);
  }

  public String getSslTruststoreFilenameFeePdf() {
    return sslTruststoreFilenameFeePdf;
  }

  public String getSslTruststorePasswordFeePdf() {
    return cipher.revealString(sslTruststorePasswordFeePdf);
  }

  public String getSslKeystoreFilenameFeePdf() {
    return sslKeystoreFilenameFeePdf;
  }

  public String getSslKeystorePasswordFeePdf() {
    return cipher.revealString(sslKeystorePasswordFeePdf);
  }

  public void setSslTruststorePasswordPdf(String sslTruststorePasswordPdf)
      throws IOException, IllegalBlockSizeException {
    this.sslTruststorePasswordPdf = cipher.sealString(sslTruststorePasswordPdf);
  }

  public void setSslKeystorePasswordPdf(String sslKeystorePasswordPdf)
      throws IOException, IllegalBlockSizeException {
    this.sslKeystorePasswordPdf = cipher.sealString(sslKeystorePasswordPdf);
  }

  public void setSslTruststorePasswordFeePdf(String sslTruststorePasswordFeePdf)
      throws IOException, IllegalBlockSizeException {
    this.sslTruststorePasswordFeePdf = cipher.sealString(sslTruststorePasswordFeePdf);
  }

  public void setSslKeystorePasswordFeePdf(String sslKeystorePasswordFeePdf)
      throws IOException, IllegalBlockSizeException {
    this.sslKeystorePasswordFeePdf = cipher.sealString(sslKeystorePasswordFeePdf);
  }

  public String getSnsTopicName() {
    return snsTopicName;
  }

  public String getSnsRoutingKey() {
    return snsRoutingKey;
  }

  public AmazonConfigBase getSnsConfiguration() {
    return snsConfiguration;
  }

  public boolean isSnsEncryptMessages() {
    return snsEncryptMessages;
  }

  public String getSnsSubject() {
    return snsSubject;
  }
}
