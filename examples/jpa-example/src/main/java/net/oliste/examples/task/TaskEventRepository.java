package net.oliste.reactive.task;

import javax.inject.Singleton;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.persistance.AggregateRepository;
import net.oliste.jpa.es.persistance.JpaPersistentEventRepository;

@Singleton
public class TaskEventRepository
    extends AggregateRepository<TaskEvent, net.oliste.reactive.task.Task> {
  public TaskEventRepository(
      JpaPersistentEventRepository persistentEventRepository,
      EventMapper eventMapper,
      EventDispatcher eventDispatcher) {
    super(persistentEventRepository, eventMapper, eventDispatcher, Task.class.getSimpleName());
  }
}
