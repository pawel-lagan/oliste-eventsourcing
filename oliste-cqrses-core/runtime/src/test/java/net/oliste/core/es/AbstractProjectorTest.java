package net.oliste.core.es;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.persistance.AggregateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class AbstractProjectorTest {

  @Mock private EventDispatcher eventDispatcher;
  @Mock private EventStore<Event, ?, ?> eventStore;

  @Mock private ObjectMapper objectMapper;
  private EventMapper eventMapper = new EventMapper(objectMapper);

  @Mock private AggregateRepository<Event, TestAggregate> repository;
  private AbstractProjector<
          Event, TestAggregate, EventStore<Event, ?, ?>, AggregateRepository<Event, TestAggregate>>
      projector;

  @BeforeEach
  void setUp() {
    projector =
        new AbstractProjector<
            Event,
            TestAggregate,
            EventStore<Event, ?, ?>,
            AggregateRepository<Event, TestAggregate>>(
            eventDispatcher, eventStore, eventMapper, repository) {};
  }

  @Test
  void projectorShouldSubscribedToEventStore() {
    var testEvent = prepareTestEvent();
    when(eventStore.toMulti()).thenReturn(Multi.createFrom().item(testEvent));
    projector.initializeProjector(TestAggregate.class);
    verify(eventStore, times(1)).toMulti();
  }

  @Test
  void stateOfAggregateShouldBeRestoredFromEvents(VertxTestContext testContext) {
    var testEvent = prepareTestEvent();
    Map<Class<?>, EventMapper.EventDescripton> eventDesc =
        Map.of(Event.class, EventMapper.EventDescripton.of(Event.class, "TestEvent", "TestEntity"));
    eventMapper.initialize(eventDesc, Collections.emptyMap());

    var testAggregate = new TestAggregate(1L, 1L);
    when(repository.loadEntity(eq(1L), eq(TestAggregate.class)))
        .thenReturn(Uni.createFrom().item(testAggregate));

    projector.taskEventHandler(testEvent, TestAggregate.class).subscribe().with((item) -> {});

    verify(repository, times(1)).loadEntity(eq(1L), eq(TestAggregate.class));
    verify(eventDispatcher, times(1)).applyEvent(testEvent, testAggregate);

    testContext.completeNow();
  }

  private static Event prepareTestEvent() {
    return Event.builder().entityId(1L).build();
  }

  @Setter
  @Getter
  @AllArgsConstructor
  private static class TestAggregate implements Aggregate<Event> {
    private Long id;
    private Long version;
  }
}
