package net.oliste.core.common.env;

public interface Environment {
  public static final String ENVIRONMENT_PROPERTY = "environment";
  public static final String PROD = "prod";

  public boolean isStage();

  public String getEnvironmentName();
}
