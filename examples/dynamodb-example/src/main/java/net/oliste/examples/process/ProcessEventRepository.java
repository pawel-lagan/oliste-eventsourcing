package net.oliste.examples.process;

import javax.inject.Singleton;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.persistance.AggregateRepository;
import net.oliste.dynamodb.es.persistance.DynamoDbPersistentEventRepository;

@Singleton
public class ProcessEventRepository extends AggregateRepository<ProcessEvent, Process> {
  public ProcessEventRepository(
      DynamoDbPersistentEventRepository persistentEventRepository,
      EventMapper eventMapper,
      EventDispatcher eventDispatcher) {
    super(persistentEventRepository, eventMapper, eventDispatcher, Process.class.getSimpleName());
  }
}
