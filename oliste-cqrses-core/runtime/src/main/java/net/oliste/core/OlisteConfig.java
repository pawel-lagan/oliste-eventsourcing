package net.oliste.core;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@Unremovable
@ConfigRoot(name = "oliste", phase = ConfigPhase.BUILD_TIME, prefix = "")
public class OlisteConfig {
  /** Package name for scanning. */
  @ConfigItem(defaultValue = "net.oliste")
  public String packageName;

  /** Package name for scanning. */
  @ConfigItem(defaultValue = "test")
  public String stage;

  /** CQRS config. */
  @ConfigItem public CqrsConfig cqrs;

  /** Event Store config. */
  @ConfigItem public EventStoreConfig es;

  @ConfigGroup
  public static class CqrsConfig {
    /** Turn on/off CQRS functionality. */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
  }

  @ConfigGroup
  public static class EventStoreConfig {
    /** Turn on/off Event Store functionality. */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
  }
}
