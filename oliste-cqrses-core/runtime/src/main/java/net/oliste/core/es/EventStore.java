package net.oliste.core.es;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.MultiEmitterProcessor;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.persistance.PersistentEvent;
import net.oliste.core.es.persistance.PersistentEventRepository;

public class EventStore<
    T extends Event, R extends PersistentEvent<?>, S extends PersistentEventRepository<R>> {
  private final Queue<T> store = new ConcurrentLinkedQueue<>();
  private final Class<T> storeType;

  private MultiEmitterProcessor<T> processor = MultiEmitterProcessor.create();
  private Multi<T> multi;

  private final EventMapper eventMapper;
  private final S persistentEventRepository;

  public EventStore(EventMapper eventMapper, S persistentEventRepository, Class<T> storeType) {
    this.storeType = storeType;
    this.eventMapper = eventMapper;
    this.persistentEventRepository = persistentEventRepository;

    multi = processor.toMulti().broadcast().toAllSubscribers();
  }

  public Uni<T> publishEvent(@NonNull T event) {
    R persistentEvent =
        eventMapper.toPersistentEntity(event, persistentEventRepository.createNewPersistentEvent());

    return persistentEventRepository
        .saveWithVersion(persistentEvent)
        .onItem()
        .transformToUni(
            resp -> {
              event.setEntityId(resp.getEntityId());
              forwardEvent(event);
              return Uni.createFrom().item(event);
            });
  }

  private T forwardEvent(T event) {
    store.add(event);
    processor.emit(event);
    return event;
  }

  public Uni<Void> loadEventStore() {
    return persistentEventRepository
        .getAll()
        .onItem()
        .invoke(
            list -> {
              final List<T> eventList =
                  list.stream()
                      .map(persistentEvent -> eventMapper.<T>toEvent(persistentEvent))
                      .map(event -> forwardEvent(event))
                      .collect(Collectors.toList());
            })
        .onItem()
        .transformToUni(list -> Uni.createFrom().voidItem());
  }

  public Uni<Void> initalize() {
    multi = processor.toMulti().broadcast().toAllSubscribers();
    return Uni.createFrom().voidItem();
  }

  public Multi<T> toMulti() {
    return multi;
  }

  public Queue<T> getStore() {
    return store;
  }
}
