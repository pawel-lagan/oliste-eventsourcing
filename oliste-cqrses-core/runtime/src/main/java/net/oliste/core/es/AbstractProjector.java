package net.oliste.core.es;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.Getter;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.persistance.AggregateRepository;

public abstract class AbstractProjector<
    E extends Event,
    A extends Aggregate<E>,
    S extends EventStore<E, ?, ?>,
    R extends AggregateRepository<E, A>> {
  @Getter(AccessLevel.PROTECTED)
  private final EventDispatcher eventDispatcher;

  @Getter(AccessLevel.PROTECTED)
  private final S eventStore;

  @Getter(AccessLevel.PROTECTED)
  private final EventMapper eventMapper;

  @Getter(AccessLevel.PROTECTED)
  private final R repository;

  protected AbstractProjector(
      EventDispatcher eventDispatcher, S eventStore, EventMapper eventMapper, R repository) {
    this.eventDispatcher = eventDispatcher;
    this.eventStore = eventStore;
    this.eventMapper = eventMapper;
    this.repository = repository;
  }

  protected Multi<A> initializeProjector(Class<A> aggregateClass) {
    return getEventStore()
        .toMulti()
        .onItem()
        .transformToUni(event -> taskEventHandler(event, aggregateClass))
        .concatenate();
  }

  public Uni<A> taskEventHandler(E event, Class<A> aggregateClass) {
    return getEventMapper()
        .<E, Uni<A>>mapClassToEventAndEntityName(
            event,
            (eventName, entityName) -> {
              return repository
                  .loadEntity(event.getEntityId(), aggregateClass)
                  .onItem()
                  .transform(
                      entity -> {
                        getEventDispatcher().applyEvent(event, entity);
                        return entity;
                      });
            });
  }
}
