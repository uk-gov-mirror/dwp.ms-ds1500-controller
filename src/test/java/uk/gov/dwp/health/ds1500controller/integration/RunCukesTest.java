package uk.gov.dwp.health.ds1500controller.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import uk.gov.dwp.health.ds1500controller.application.Ds1500ControllerApplication;
import uk.gov.dwp.health.ds1500controller.application.Ds1500ControllerConfiguration;

import java.io.File;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings({"squid:S1118", "squid:S2187"}) // no private constructor required
@CucumberOptions(plugin = "json:target/cucumber-report.json")
public class RunCukesTest {

  private static final String CONFIG_FILE = "test.yml";

  @ClassRule
  public static final DropwizardAppRule<Ds1500ControllerConfiguration> RULE =
      new DropwizardAppRule<>(Ds1500ControllerApplication.class, resourceFilePath(CONFIG_FILE));
}
