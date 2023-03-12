package net.oliste.reactive.task;

import io.quarkus.runtime.StartupEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import net.oliste.core.es.AbstractProjector;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventHandler;
import net.oliste.core.es.events.EventMapper;

@Singleton
public class TaskProjector
    extends AbstractProjector<
        TaskEvent, net.oliste.reactive.task.Task, TaskEventStore, TaskEventRepository> {
  private final Map<Long, net.oliste.reactive.task.Task> projectionCache =
      new ConcurrentHashMap<>();

  public TaskProjector(
      TaskEventRepository taskEventRepository,
      TaskEventStore eventStore,
      EventDispatcher eventDispatcher,
      EventMapper eventMapper) {
    super(eventDispatcher, eventStore, eventMapper, taskEventRepository);
  }

  void startup(@Observes StartupEvent startupEvent) {
    initializeProjector(net.oliste.reactive.task.Task.class)
        .subscribe()
        .with(
            aggregate -> {
              projectionCache.put(aggregate.getId(), aggregate);
            });
  }

  public Map<Long, net.oliste.reactive.task.Task> getProjectionCache() {
    return projectionCache;
  }

  @EventHandler
  public void handleEntityEvent(net.oliste.reactive.task.Task.TaskUpdateEvent event, Task entity) {
    System.out.println("Applying event " + event + " on " + entity);
  }
}
