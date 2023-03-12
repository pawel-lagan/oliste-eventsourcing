package net.oliste.examples.process;

import io.quarkus.runtime.StartupEvent;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import net.oliste.core.es.EventStore;
import net.oliste.core.es.events.EventMapper;
import net.oliste.dynamodb.es.persistance.DynamoDbPersistentEventRepository;
import net.oliste.dynamodb.es.persistance.PersistentEvent;

@Singleton
public class ProcessEventStore
    extends EventStore<ProcessEvent, PersistentEvent, DynamoDbPersistentEventRepository> {

  public ProcessEventStore(
      EventMapper eventMapper, DynamoDbPersistentEventRepository persistentEventRepository) {
    super(eventMapper, persistentEventRepository, ProcessEvent.class);
  }

  void startup(@Observes StartupEvent event, ProcessEventStore processEventStore) {
    processEventStore.initalize().await().indefinitely();
  }
}
