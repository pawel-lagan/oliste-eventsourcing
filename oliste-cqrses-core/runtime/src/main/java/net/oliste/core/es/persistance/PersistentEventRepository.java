package net.oliste.core.es.persistance;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public interface PersistentEventRepository<T extends PersistentEvent<?>> {

  Uni<List<T>> getAll();

  Uni<List<T>> getAllByEntity(String entityName);

  Uni<List<T>> getAllByEntityId(String entityName, Long entityId);

  Uni<Long> getNextEntityId(String entityName);

  Uni<Long> getNextEntityVersion(String entityName, long entityId);

  Uni<Long> getLastEntityVersion(String entityName, long entityId);

  Uni<T> saveWithVersion(T event);

  T createNewPersistentEvent();
}
