package net.oliste.core.es.persistance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import io.smallrye.mutiny.Uni;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class AggregateRepositoryTest {
  @Mock private EventDispatcher eventDispatcher;
  @Mock private PersistentEventRepository<TestPersistentEvent> persistentEventRepository;
  @Mock private EventMapper eventMapper;

  private AggregateRepository<Event, TestAggregate> aggregateRepository;

  @BeforeEach
  void setUp() {
    aggregateRepository =
        new AggregateRepository<>(
            persistentEventRepository, eventMapper, eventDispatcher, "TestEntity");
  }

  @Test
  void shouldRecreateAggregateStateByReplyingEventsForSingleEntity(VertxTestContext testContext) {
    var persistentEvent = prepareTestPersistentEvent(1L);
    var listOfPersistentEvents = Uni.createFrom().item(List.of(persistentEvent));
    when(persistentEventRepository.getAllByEntityId(any(), eq(1L)))
        .thenReturn(listOfPersistentEvents);

    var testEvent = prepareTestEvent();
    when(eventMapper.toEvent(persistentEvent)).thenReturn(testEvent);

    aggregateRepository
        .loadEntity(1L, TestAggregate.class)
        .subscribe()
        .with(
            result -> {
              Assertions.assertThat(result).isInstanceOf(TestAggregate.class);
              Assertions.assertThat(result.getId()).isEqualTo(1L);

              verify(eventMapper, times(1)).toEvent(persistentEvent);
              testContext.completeNow();
            },
            t -> testContext.failNow(t));
  }

  @Test
  void shouldRecreateAggregateStateByReplyingEventsForAllEntities(VertxTestContext testContext) {
    TestPersistentEvent persistentEvent = prepareTestPersistentEvent(1L);
    TestPersistentEvent persistentEvent2 = prepareTestPersistentEvent(2L);
    var listOfPersistentEvents = Uni.createFrom().item(List.of(persistentEvent, persistentEvent2));
    when(persistentEventRepository.getAllByEntity(any())).thenReturn(listOfPersistentEvents);

    var testEvent = prepareTestEvent();
    when(eventMapper.toEvent(persistentEvent)).thenReturn(testEvent);

    aggregateRepository
        .loadAllEntities(TestAggregate.class)
        .subscribe()
        .with(
            result -> {
              Assertions.assertThat(result).isNotEmpty().hasSize(2);
              Assertions.assertThat(result)
                  .extracting(TestAggregate::getId)
                  .containsExactly(1L, 2L);
              Assertions.assertThat(result)
                  .extracting(TestAggregate::getVersion)
                  .containsExactly(1L, 1L);

              verify(eventMapper, times(1)).toEvent(persistentEvent);
              testContext.completeNow();
            },
            t -> testContext.failNow(t));
  }

  private static TestPersistentEvent prepareTestPersistentEvent(long entityId) {
    var persistentEvent = new TestPersistentEvent();
    persistentEvent.setEntityId(entityId);
    persistentEvent.setVersion(1L);
    return persistentEvent;
  }

  private static Event prepareTestEvent() {
    return Event.builder().entityId(1L).build();
  }

  @Setter
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  private static class TestAggregate implements Aggregate<Event> {
    private Long id;
    private Long version;
  }

  @Setter
  @Getter
  private static class TestPersistentEvent implements PersistentEvent<Long> {
    private Long id;
    private Long entityId;
    private String entityName;
    private String eventName;
    private String traceId;
    private String spanId;
    private String parentId;
    private LocalDateTime createdAt;
    private String eventData;
    private Long version;
  }
}
