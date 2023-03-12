package net.oliste.examples.process;

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
public class ProcessProjector
    extends AbstractProjector<ProcessEvent, Process, ProcessEventStore, ProcessEventRepository> {
  private final Map<Long, Process> projectionCache = new ConcurrentHashMap<>();

  public ProcessProjector(
      ProcessEventRepository processEventRepository,
      ProcessEventStore eventStore,
      EventDispatcher eventDispatcher,
      EventMapper eventMapper) {
    super(eventDispatcher, eventStore, eventMapper, processEventRepository);
  }

  void startup(@Observes StartupEvent startupEvent) {
    initializeProjector(Process.class)
        .subscribe()
        .with(
            aggregate -> {
              projectionCache.put(aggregate.getId(), aggregate);
            });
  }

  public Map<Long, Process> getProjectionCache() {
    return projectionCache;
  }

  @EventHandler
  public void handleEntityEvent(Process.ProcessUpdateEvent event, Process entity) {
    System.out.println("Applying event " + event + " on " + entity);
  }
}
