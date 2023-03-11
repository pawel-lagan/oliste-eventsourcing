package net.oliste.core.es.persistance;

import io.smallrye.mutiny.Uni;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;

@RequiredArgsConstructor
public class AggregateRepository<T extends Event, E extends Aggregate> {
  private final PersistentEventRepository<? extends PersistentEvent<?>> persistentEventRepository;
  private final EventMapper eventMapper;
  private final EventDispatcher eventDispatcher;
  private final String entityName;

  public Uni<E> loadEntity(long id, Class<E> entityClass) {
    return persistentEventRepository
        .getAllByEntityId(entityName, id)
        .onItem()
        .transform(
            list -> {
              List<T> eventList =
                  list.stream()
                      .map(persistentEvent -> eventMapper.<T>toEvent(persistentEvent))
                      .collect(Collectors.toList());
              return createEntity(
                  id, list.get(list.size() - 1).getVersion(), entityClass, eventList);
            });
  }

  public Uni<List<E>> loadAllEntities(Class<E> entityClass) {
    return persistentEventRepository
        .getAllByEntity(entityName)
        .onItem()
        .transform(
            list -> {
              final Map<Long, List<T>> eventMap =
                  list.stream()
                      .collect(
                          Collectors.groupingBy(
                              PersistentEvent::getEntityId,
                              HashMap::new,
                              Collectors.mapping(
                                  event -> eventMapper.<T>toEvent(event), Collectors.toList())));

              final Map<Long, Optional<PersistentEvent>> latestEventMap =
                  list.stream()
                      .collect(
                          Collectors.groupingBy(
                              PersistentEvent::getEntityId,
                              Collectors.reducing(
                                  BinaryOperator.maxBy(
                                      Comparator.comparingLong(PersistentEvent::getVersion)))));
              return eventMap.entrySet().stream()
                  .map(
                      entry -> {
                        Long version = latestEventMap.get(entry.getKey()).get().getVersion();
                        return createEntity(entry.getKey(), version, entityClass, entry.getValue());
                      })
                  .collect(Collectors.toList());
            });
  }

  private E createEntity(long id, Long version, Class<E> entityClass, List<T> eventList) {
    E entity = null;
    try {
      entity = entityClass.getConstructor().newInstance();
      entity.setId(id);
      entity.setVersion(version);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException ex) {
      throw new RuntimeException("Entity load error " + entityName + "#" + id, ex);
    }
    eventDispatcher.applyEvents(eventList, entity);
    return entity;
  }
}
