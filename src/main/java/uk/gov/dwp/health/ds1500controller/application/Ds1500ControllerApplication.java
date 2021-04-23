package uk.gov.dwp.health.ds1500controller.application;

import com.amazonaws.services.sns.model.MessageAttributeValue;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.MessageEncoder;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.ds1500controller.Ds1500ControllerResource;
import uk.gov.dwp.health.ds1500controller.Ds1500JsonValidator;
import uk.gov.dwp.health.ds1500controller.MetadataBuilder;
import uk.gov.dwp.health.ds1500controller.utils.PdfRetriever;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.version.HealthCheckResource;
import uk.gov.dwp.health.version.ServiceInfoResource;
import uk.gov.dwp.health.version.info.PropertyFileInfoProvider;
import uk.gov.dwp.tls.TLSConnectionBuilder;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Ds1500ControllerApplication extends Application<Ds1500ControllerConfiguration> {

  @Override
  protected void bootstrapLogging() {
    // to prevent dropwizard using its own standard logger
  }

  @Override
  public void run(
      Ds1500ControllerConfiguration ds1500ControllerConfiguration, Environment environment)
      throws CryptoException, IllegalBlockSizeException, InvalidKeyException,
      NoSuchPaddingException, NoSuchAlgorithmException, IOException {

    CryptoDataManager kmsCrypto = null;
    if (ds1500ControllerConfiguration.isSnsEncryptMessages()
        && null == ds1500ControllerConfiguration.getKmsCryptoConfig()) {
      throw new CryptoException(
          "SnsEncryptMessages is TRUE.  "
              + "Cannot encrypt without a valid 'kmsCryptoConfiguration' configuration item");

    } else if (ds1500ControllerConfiguration.isSnsEncryptMessages()) {
      kmsCrypto = new CryptoDataManager(ds1500ControllerConfiguration.getKmsCryptoConfig());
    }

    final MessageEncoder<MessageAttributeValue> messageEncoder =
        new MessageEncoder<>(kmsCrypto, MessageAttributeValue.class);
    final MessagePublisher snsPublisher =
        new MessagePublisher(messageEncoder, ds1500ControllerConfiguration.getSnsConfiguration());

    final TLSConnectionBuilder pdfSslConnection =
        new TLSConnectionBuilder(
            ds1500ControllerConfiguration.getSslTruststoreFilenamePdf(),
            ds1500ControllerConfiguration.getSslTruststorePasswordPdf(),
            ds1500ControllerConfiguration.getSslKeystoreFilenamePdf(),
            ds1500ControllerConfiguration.getSslKeystorePasswordPdf());

    final TLSConnectionBuilder feePdfSslConnection =
        new TLSConnectionBuilder(
            ds1500ControllerConfiguration.getSslTruststoreFilenameFeePdf(),
            ds1500ControllerConfiguration.getSslTruststorePasswordFeePdf(),
            ds1500ControllerConfiguration.getSslKeystoreFilenameFeePdf(),
            ds1500ControllerConfiguration.getSslKeystorePasswordFeePdf());

    final Ds1500ControllerResource instance =
        new Ds1500ControllerResource(
            ds1500ControllerConfiguration,
            snsPublisher,
            new PdfRetriever(pdfSslConnection),
            new PdfRetriever(feePdfSslConnection),
            new Ds1500JsonValidator(),
            new MetadataBuilder());

    environment.jersey().register(instance);
    environment.jersey().register(new HealthCheckResource());

    if (ds1500ControllerConfiguration.isApplicationInfoEnabled()) {
      environment.jersey()
          .register(
              new ServiceInfoResource(
                  new PropertyFileInfoProvider("application.yml")
              )
          );
    }

  }

  @Override
  public void initialize(Bootstrap<Ds1500ControllerConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        )
    );
  }

  public static void main(String[] args) throws Exception {
    new Ds1500ControllerApplication().run(args);
  }
}
