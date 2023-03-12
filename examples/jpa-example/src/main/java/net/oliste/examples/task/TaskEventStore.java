package net.oliste.reactive.task;

import io.quarkus.runtime.StartupEvent;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import net.oliste.core.es.EventStore;
import net.oliste.core.es.events.EventMapper;
import net.oliste.jpa.es.persistance.JpaPersistentEventRepository;
import net.oliste.jpa.es.persistance.PersistentEvent;

@Singleton
public class TaskEventStore
    extends EventStore<TaskEvent, PersistentEvent, JpaPersistentEventRepository> {

  public TaskEventStore(
      EventMapper eventMapper, JpaPersistentEventRepository persistentEventRepository) {
    super(eventMapper, persistentEventRepository, TaskEvent.class);
  }

  void startup(@Observes StartupEvent event, TaskEventStore taskEventStore) {
    taskEventStore.initalize().await().indefinitely();
  }
}
