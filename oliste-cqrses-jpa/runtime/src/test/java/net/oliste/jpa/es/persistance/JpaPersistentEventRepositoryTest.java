package net.oliste.jpa.es.persistance;

import io.quarkus.arc.DefaultBean;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import java.time.LocalDateTime;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.core.common.time.ShiftedTimeService;
import org.assertj.core.api.Assertions;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresqlResource.class)
class JpaPersistentEventRepositoryTest {
  public static final long ENTITY_ID = 100L;
  public static final String ENTITY_1 = "Entity1";
  @Inject JpaPersistentEventRepository jpaPersistentEventRepository;
  @Inject Mutiny.SessionFactory sessionFactory;

  @Test
  @TestReactiveTransaction
  @RunOnVertxContext
  void eventWithEmptyEntityIdWillCreateNewEntity(UniAsserter uniAsserter) {
    PersistentEvent event = createTestPersistanceEvent(null, ENTITY_1);

    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  t.markForRollback();
                  return jpaPersistentEventRepository
                      .saveWithVersion(event)
                      .onItem()
                      .transformToUni(saved -> jpaPersistentEventRepository.getAll());
                }),
        (result) -> {
          Assertions.assertThat(result)
              .isNotEmpty()
              .extracting(PersistentEvent::getEntityId)
              .isNotNull();
          Assertions.assertThat(result)
              .isNotEmpty()
              .extracting(PersistentEvent::getVersion)
              .containsExactly(0L);
        });
  }

  @Test
  @TestReactiveTransaction
  @RunOnVertxContext
  void lastVersionShouldBeFound(UniAsserter uniAsserter) {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);

    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  uniAsserter.assertThat(
                      () -> jpaPersistentEventRepository.saveWithVersion(event),
                      result1 -> {
                        Assertions.assertThat(result1.getVersion()).isEqualTo(1);

                        uniAsserter.assertThat(
                            () ->
                                jpaPersistentEventRepository
                                    .saveWithVersion(event)
                                    .onItem()
                                    .transformToUni(
                                        saved ->
                                            jpaPersistentEventRepository.getLastEntityVersion(
                                                event.getEntityName(), event.getEntityId())),
                            result2 -> {
                              Assertions.assertThat(result2).isEqualTo(2);

                              uniAsserter.assertThat(
                                  () -> jpaPersistentEventRepository.getAll(),
                                  all -> {
                                    Assertions.assertThat(all)
                                        .extracting(PersistentEvent::getEntityId)
                                        .containsExactly(ENTITY_ID);
                                    Assertions.assertThat(all)
                                        .extracting(PersistentEvent::getVersion)
                                        .containsExactly(2L);
                                  });
                            });
                      });
                  t.markForRollback();
                  return Uni.createFrom().voidItem();
                }),
        toIgnore -> {});
  }

  @Test
  @TestReactiveTransaction
  @RunOnVertxContext
  void eventWithEntityIdWillCreateNewVersion(UniAsserter uniAsserter) {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);
    PersistentEvent event2 = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);

    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  uniAsserter.assertThat(
                      () ->
                          jpaPersistentEventRepository
                              .saveWithVersion(event)
                              .onItem()
                              .transformToUni(saved -> jpaPersistentEventRepository.getAll()),
                      result -> {
                        Assertions.assertThat(result)
                            .isNotEmpty()
                            .extracting(PersistentEvent::getEntityId)
                            .containsExactly(ENTITY_ID);

                        Assertions.assertThat(result)
                            .isNotEmpty()
                            .extracting(PersistentEvent::getVersion)
                            .containsExactly(1L);

                        uniAsserter.assertThat(
                            () ->
                                jpaPersistentEventRepository
                                    .saveWithVersion(event2)
                                    .onItem()
                                    .transformToUni(saved -> jpaPersistentEventRepository.getAll()),
                            result2 -> {
                              Assertions.assertThat(result2)
                                  .hasSize(2)
                                  .extracting(PersistentEvent::getEntityId)
                                  .containsExactly(ENTITY_ID, ENTITY_ID);
                              Assertions.assertThat(result2)
                                  .isNotEmpty()
                                  .extracting(PersistentEvent::getVersion)
                                  .containsExactly(1L, 2L);
                            });
                      });
                  t.markForRollback();
                  return Uni.createFrom().voidItem();
                }),
        toIgnore -> {});
  }

  @Test
  @TestReactiveTransaction
  @RunOnVertxContext
  void allEventsForSpecifiedEntityWillBeReturned(UniAsserter uniAsserter) {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);
    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  t.markForRollback();
                  return jpaPersistentEventRepository
                      .saveWithVersion(event)
                      .onItem()
                      .transformToUni(
                          saved ->
                              jpaPersistentEventRepository.getAllByEntityId(ENTITY_1, ENTITY_ID));
                }),
        result -> {
          Assertions.assertThat(result)
              .hasSize(1)
              .extracting(PersistentEvent::getEntityId)
              .containsExactly(ENTITY_ID);
        });
  }

  @Test
  @TestReactiveTransaction
  @RunOnVertxContext
  void allEventsForSpecifiedEntityNameWillBeReturned(UniAsserter uniAsserter) {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);
    PersistentEvent event2 = createTestPersistanceEvent(ENTITY_ID, "Entity2");

    var resultUni =
        sessionFactory.withTransaction(
            (s, t) -> {
              t.markForRollback();
              return jpaPersistentEventRepository
                  .saveWithVersion(event2)
                  .onItem()
                  .transformToUni(
                      saved ->
                          jpaPersistentEventRepository
                              .saveWithVersion(event)
                              .onItem()
                              .transformToUni(
                                  saved2 ->
                                      jpaPersistentEventRepository.getAllByEntityId(
                                          ENTITY_1, ENTITY_ID)));
            });

    uniAsserter.assertThat(
        () -> resultUni,
        result -> {
          Assertions.assertThat(result)
              .hasSize(1)
              .extracting(PersistentEvent::getEntityId)
              .containsExactly(ENTITY_ID);
        });
  }

  @Test
  @RunOnVertxContext
  void getNextVersionShouldReturnSequenceOfVersions(UniAsserter uniAsserter) {
    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  t.markForRollback();

                  uniAsserter.assertThat(
                      () -> jpaPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID),
                      result -> {
                        Assertions.assertThat(result).isEqualTo(0L);

                        uniAsserter.assertThat(
                            () ->
                                jpaPersistentEventRepository.getNextEntityVersion(
                                    ENTITY_1 + "mod", ENTITY_ID),
                            result2 -> {
                              Assertions.assertThat(result2).isEqualTo(0L);

                              uniAsserter.assertThat(
                                  () ->
                                      jpaPersistentEventRepository.getNextEntityVersion(
                                          ENTITY_1, ENTITY_ID),
                                  result3 -> {
                                    Assertions.assertThat(result3).isEqualTo(1L);

                                    uniAsserter.assertThat(
                                        () ->
                                            jpaPersistentEventRepository.getNextEntityVersion(
                                                ENTITY_1, ENTITY_ID),
                                        result4 -> {
                                          Assertions.assertThat(result4).isEqualTo(2L);

                                          uniAsserter.assertThat(
                                              () ->
                                                  jpaPersistentEventRepository.getNextEntityVersion(
                                                      ENTITY_1 + "mod", ENTITY_ID),
                                              result5 -> {
                                                Assertions.assertThat(result5).isEqualTo(1L);
                                              });
                                        });
                                  });
                            });
                      });

                  return Uni.createFrom().voidItem();
                }),
        toIgnore -> {});
  }

  @Test
  @RunOnVertxContext
  void shouldNotFaileWhenGetNextVersionRunsWithRaceCondidtion(UniAsserter uniAsserter) {
    uniAsserter.assertThat(
        () ->
            sessionFactory.withTransaction(
                (s, t) -> {
                  for (var i = 0; i < 300; i++) {
                    uniAsserter.assertThat(
                        () ->
                            jpaPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID),
                        result -> {
                          Assertions.assertThat(result).isGreaterThanOrEqualTo(0);
                        });

                    uniAsserter.assertThat(
                        () ->
                            jpaPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID),
                        result2 -> {
                          Assertions.assertThat(result2).isGreaterThanOrEqualTo(0);
                          ;
                        });
                  }
                  ;

                  t.markForRollback();
                  return Uni.createFrom().voidItem();
                }),
        toIgnore -> {});
  }

  private static PersistentEvent createTestPersistanceEvent(Long entityId, String entityName) {
    var event = PersistentEvent.newInstance();
    event.setEventData("{DD}");
    event.setEventName("Event name " + entityId + LocalDateTime.now());
    event.setEntityId(entityId);
    event.setEntityName(entityName);
    event.setCreatedAt(LocalDateTime.now());
    event.setParentId("AB");
    event.setSpanId("BC");
    event.setTraceId("TC");
    return event;
  }

  @Dependent
  public static class AppConfig {
    @ApplicationScoped
    @DefaultBean
    public ShiftedTimeService timeService() {
      return new ShiftedTimeService();
    }

    @ApplicationScoped
    @DefaultBean
    public Snowflake snowflake(Mutiny.SessionFactory sessionFactory, Snowflake snowflake) {
      return new Snowflake();
    }

    @ApplicationScoped
    @DefaultBean
    public JpaPersistentEventRepository initializeEventStoreRepository(
        Mutiny.SessionFactory sessionFactory, Snowflake snowflake) {
      return new JpaPersistentEventRepository(sessionFactory, snowflake);
    }

    @ApplicationScoped
    DispatcherHelper dispatcherHelper() {
      return new DispatcherHelper();
    }
  }
}
