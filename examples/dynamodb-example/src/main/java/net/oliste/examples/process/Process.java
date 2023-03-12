package net.oliste.examples.process;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import net.oliste.core.es.events.EventHandler;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.EventDescriptor;

public class Process implements Aggregate<ProcessEvent> {
  @Getter @Setter private Long id;

  @Getter @Setter private Long version;

  @Getter @Setter private LocalDateTime createdAt;
  static final String EVENT_STORE_NAME = "Process";

  @Getter @Setter private String name;

  public Process() {}

  public static Process newInstance() {
    return new Process();
  }

  @EventHandler
  public void updateTask(ProcessUpdateEvent event) {
    setName(event.getName());
  }

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "ProcessUpdateEvent", entityName = Process.EVENT_STORE_NAME)
  public static class ProcessUpdateEvent extends ProcessEvent {
    private final String name;
  }
}
