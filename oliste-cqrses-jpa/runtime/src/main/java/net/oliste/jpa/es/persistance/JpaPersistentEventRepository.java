package net.oliste.jpa.es.persistance;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import java.time.LocalDateTime;
import java.util.List;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.core.es.persistance.PersistentEventRepository;
import org.hibernate.LockMode;
import org.hibernate.reactive.mutiny.Mutiny;

@Unremovable
public class JpaPersistentEventRepository
    implements PersistentEventRepository<PersistentEvent>, PanacheRepository<PersistentEvent> {

  protected final Mutiny.SessionFactory sessionFactory;
  private final Snowflake snowFlake;

  public JpaPersistentEventRepository(Mutiny.SessionFactory sessionFactory, Snowflake snowFlake) {
    this.sessionFactory = sessionFactory;
    this.snowFlake = snowFlake;
  }

  public Uni<PersistentEvent> save(PersistentEvent event) {
    return persistAndFlush(event);
  }

  public Uni<List<PersistentEvent>> getAll() {
    return listAll(Sort.descending("id"));
  }

  public Uni<List<PersistentEvent>> getAllByEntity(String entityName) {
    return find(
            "entityName = :name", Sort.descending("entityId"), Parameters.with("name", entityName))
        .list();
  }

  public Uni<List<PersistentEvent>> getAllByEntityId(String entityName, Long entityId) {
    Parameters parameters = Parameters.with("name", entityName).and("id", entityId);
    return find("entityName = :name AND entityId = :id", Sort.descending("entityId"), parameters)
        .list();
  }

  @Override
  public Uni<Long> getNextEntityId(String entityName) {
    return Uni.createFrom().item(snowFlake.nextId());
  }

  @Override
  public Uni<Long> getNextEntityVersion(String entityName, long entityId) {
    return sessionFactory.withTransaction(
        (s, t) -> {
          Uni<PersistentEventVersion> get =
              s.find(
                  PersistentEventVersion.class,
                  new PersistentEventVersion.PersistentEventVersionPk(entityId, entityName),
                  LockMode.OPTIMISTIC);
          return get.onItem()
              .transformToUni(
                  inst -> {
                    var toPersist = inst != null ? inst : PersistentEventVersion.newInstance();
                    if (inst == null) {
                      toPersist.setEntityId(entityId);
                      toPersist.setEntityName(entityName);
                    }
                    toPersist.setTimestamp(LocalDateTime.now());
                    var q =
                        s.persist(toPersist)
                            .onItem()
                            .transformToUni(
                                r ->
                                    s.flush()
                                        .onItem()
                                        .transformToUni(
                                            qv -> Uni.createFrom().item(toPersist.getVersion())));
                    return q;
                  });
        });
  }

  public Uni<Long> getLastEntityVersion(String entityName, long entityId) {
    return sessionFactory.withSession(
        (s) -> {
          Mutiny.Query<Long> query =
              s.createQuery(
                      "SELECT max(version)"
                          + " FROM PersistentEvent"
                          + " WHERE entityName = :entityName AND entityId = :entityId",
                      Long.class)
                  .setParameter("entityName", entityName)
                  .setParameter("entityId", entityId);
          return query.getSingleResult();
        });
  }

  @Override
  public PersistentEvent createNewPersistentEvent() {
    return PersistentEvent.newInstance();
  }

  public Uni<PersistentEvent> saveWithVersion(PersistentEvent event) {
    return sessionFactory.withTransaction(
        (s, t) -> {
          if (event.getEntityId() == null) {
            return getNextEntityId(event.getEntityName())
                .onItem()
                .transformToUni(
                    id -> {
                      return getNextEntityVersion(event.getEntityName(), id)
                          .onItem()
                          .transformToUni(
                              version -> {
                                event.setEntityId(id);
                                event.setVersion(version);
                                return persistAndFlush(event);
                              });
                    });
          }
          return getNextEntityVersion(event.getEntityName(), event.getEntityId())
              .onItem()
              .transformToUni(
                  version -> {
                    event.setVersion(version);
                    return persistAndFlush(event);
                  });
        });
  }
}
