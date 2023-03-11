package net.oliste.core.common.env;

import io.quarkus.arc.properties.UnlessBuildProperty;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@UnlessBuildProperty(name = Environment.ENVIRONMENT_PROPERTY, stringValue = Environment.PROD)
public class StageEnvironment implements Environment {
  @ConfigProperty(name = Environment.ENVIRONMENT_PROPERTY, defaultValue = "Stage")
  String envName;

  @Override
  public boolean isStage() {
    return true;
  }

  @Override
  public String getEnvironmentName() {
    return envName;
  }
}
