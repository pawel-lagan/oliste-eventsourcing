package net.oliste.reactive.task;

import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import net.oliste.core.es.events.EventHandler;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.EventDescriptor;
import net.oliste.jpa.hibernate.BaseEntity;

@Entity
public class Task extends BaseEntity implements Aggregate<TaskEvent> {

  static final String EVENT_STORE_NAME = "Task";

  @Getter @Setter private String name;

  public Task() {
    this(false);
  }

  private Task(boolean generateUuid) {
    super(generateUuid);
  }

  public static Task newInstance() {
    return new Task(true);
  }

  @EventHandler
  public void updateTask(TaskUpdateEvent event) {
    setName(event.getName());
  }

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "TaskUpdateEvent", entityName = Task.EVENT_STORE_NAME)
  public static class TaskUpdateEvent extends TaskEvent {
    private final String name;
  }
}
