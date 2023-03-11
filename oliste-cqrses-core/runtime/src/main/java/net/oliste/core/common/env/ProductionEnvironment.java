package net.oliste.core.common.env;

import io.quarkus.arc.properties.IfBuildProperty;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProperty(name = Environment.ENVIRONMENT_PROPERTY, stringValue = Environment.PROD)
public class ProductionEnvironment implements Environment {
  @Override
  public boolean isStage() {
    return false;
  }

  @Override
  public String getEnvironmentName() {
    return "Production";
  }
}
