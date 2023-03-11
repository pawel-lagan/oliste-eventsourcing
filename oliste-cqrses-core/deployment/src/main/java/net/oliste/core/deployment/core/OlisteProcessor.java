package net.oliste.core.deployment.core;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.core.common.time.RealTimeService;
import net.oliste.core.common.time.ShiftedTimeService;

class OlisteProcessor {
  private static final String OLISTE = "oliste";
  public static final String PROD = "prod";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(OLISTE);
  }

  @BuildStep
  AdditionalBeanBuildItem additionalBeans(OlisteConfig olisteConfig) {
    Class<?> timeServiceClass = ShiftedTimeService.class;
    if (PROD.equals(olisteConfig.stage)) {
      timeServiceClass = RealTimeService.class;
    }

    return AdditionalBeanBuildItem.builder()
        .setUnremovable()
        .addBeanClasses(timeServiceClass)
        .build();
  }

  @BuildStep
  AdditionalBeanBuildItem snowflakeBean() {
    return AdditionalBeanBuildItem.builder()
        .setUnremovable()
        .addBeanClasses(Snowflake.class)
        .build();
  }
}
