package net.oliste.core.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.persistance.PersistentEvent;
import net.oliste.core.es.persistance.PersistentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class EventStoreTest {

  @Mock EventMapper eventMapperMock;

  @Mock PersistentEventRepository persistentEventRepositoryMock;

  private EventStoreFixture fixture = new EventStoreFixture();

  @BeforeEach
  public void setup() {
    when(persistentEventRepositoryMock.saveWithVersion(any()))
        .thenReturn(Uni.createFrom().item(mock(TestPersistentEvent.class)));
  }

  @Test
  @DisplayName("Event should be added to event store")
  public void shouldAddEventToStore(Vertx vertx, VertxTestContext testContext)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException,
          IllegalAccessException {
    TestEventStore store = fixture.emptyTestStore(eventMapperMock, persistentEventRepositoryMock);
    TestEvent testEvent = TestEvent.builder().entityId(342340423423L).value("event1").build();

    Multi<TestEvent> eventsMulti = store.toMulti();
    AssertSubscriber s1 = AssertSubscriber.create(2);

    store.toMulti().subscribe().withSubscriber(s1);

    eventsMulti
        .onSubscription()
        .invoke(
            () -> {
              System.out.println("Subscribed");
            })
        .onItem()
        .call(
            event -> {
              System.out.println(event);
              return Uni.createFrom()
                  .emitter(
                      e -> {
                        e.complete(event);
                      });
            })
        .subscribe()
        .with(
            event -> {
              System.out.println("Subscriber get = " + event);
            },
            fail -> {
              System.out.println("Fail");
            },
            () -> {
              System.out.println("Completed");
            });

    store.publishEvent(testEvent).subscribe().with((item) -> {});

    Multi<TestEvent> eventsMulti2 = store.toMulti();
    eventsMulti2
        .onSubscription()
        .invoke(
            () -> {
              System.out.println("2nd Subscribed");
            })
        .onItem()
        .call(
            event -> {
              System.out.println("2nd " + event);
              return Uni.createFrom()
                  .emitter(
                      e -> {
                        e.complete(event);
                      });
            })
        .subscribe()
        .with(
            event -> {
              System.out.println("2nd Subscriber get = " + event);
            },
            fail -> {
              System.out.println("2nd Fail");
            },
            () -> {
              System.out.println("2nd Completed");
            });

    TestEvent event2 = TestEvent.builder().entityId(342340423423L).value("event2").build();

    store.publishEvent(event2).subscribe().with((item) -> {});

    assertThat(s1.getItems()).containsExactly(testEvent, event2);

    testContext.completeNow();
  }

  @Test
  @DisplayName("Event should be added to hot flux")
  public void shouldNotExpectEventInStore(Vertx vertx, VertxTestContext testContext) {
    TestEventStore store = fixture.emptyTestStore(eventMapperMock, persistentEventRepositoryMock);
    TestEvent testEvent = TestEvent.builder().entityId(342340423423L).value("event1").build();

    store.publishEvent(testEvent).subscribe().with((item) -> {});

    AssertSubscriber s1 = AssertSubscriber.create(2);
    store.toMulti().subscribe().withSubscriber(s1);

    assertThat(s1.getItems()).containsExactly(testEvent);

    testContext.completeNow();
  }

  @Test
  @DisplayName("Event get all events for entity")
  public void shouldGetAllEventsForEntity(Vertx vertx, VertxTestContext testContext)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException,
          IllegalAccessException {
    TestEventStore store = fixture.emptyTestStore(eventMapperMock, persistentEventRepositoryMock);
    TestEvent testEvent = TestEvent.builder().entityId(342340423423L).value("event1").build();
    TestEvent testEvent2 = TestEvent.builder().entityId(342340423423L).value("event2").build();
    TestEvent testEvent3 = TestEvent.builder().entityId(213123123121L).value("event3").build();

    store.publishEvent(testEvent).subscribe().with((item) -> {});
    store.publishEvent(testEvent2).subscribe().with((item) -> {});
    store.publishEvent(testEvent3).subscribe().with((item) -> {});

    AssertSubscriber s1 = AssertSubscriber.create(3);
    store.toMulti().subscribe().withSubscriber(s1);

    assertThat(s1.getItems()).containsExactly(testEvent, testEvent2, testEvent3);

    testContext.completeNow();
  }

  @Getter
  @SuperBuilder
  static class TestEvent extends Event {
    private final String value;
  }

  private class TestEventStore
      extends EventStore<
          TestEvent, TestPersistentEvent, PersistentEventRepository<TestPersistentEvent>> {
    public TestEventStore(
        EventMapper eventMapper,
        PersistentEventRepository persistentEventRepository,
        Class<TestEvent> storeType) {
      super(eventMapper, persistentEventRepository, storeType);
    }
  }

  private class EventStoreFixture {

    public TestEventStore emptyTestStore(
        EventMapper eventMapper, PersistentEventRepository persistentEventRepository) {
      return new TestEventStore(eventMapper, persistentEventRepository, TestEvent.class);
    }
  }

  private class TestPersistentEvent implements PersistentEvent<Long> {

    @Override
    public Long getId() {
      return null;
    }

    @Override
    public Long getEntityId() {
      return null;
    }

    @Override
    public String getEntityName() {
      return null;
    }

    @Override
    public String getEventName() {
      return null;
    }

    @Override
    public String getTraceId() {
      return null;
    }

    @Override
    public String getSpanId() {
      return null;
    }

    @Override
    public String getParentId() {
      return null;
    }

    @Override
    public LocalDateTime getCreatedAt() {
      return null;
    }

    @Override
    public String getEventData() {
      return null;
    }

    @Override
    public Long getVersion() {
      return null;
    }

    @Override
    public void setEntityId(Long entityId) {}

    @Override
    public void setEntityName(String entityName) {}

    @Override
    public void setEventName(String eventName) {}

    @Override
    public void setTraceId(String traceId) {}

    @Override
    public void setSpanId(String spanId) {}

    @Override
    public void setParentId(String parentId) {}

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {}

    @Override
    public void setEventData(String eventData) {}

    @Override
    public void setVersion(Long version) {}
  }
}
