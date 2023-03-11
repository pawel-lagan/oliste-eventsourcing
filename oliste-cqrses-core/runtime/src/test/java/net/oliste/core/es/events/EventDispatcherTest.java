package net.oliste.core.es.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import net.oliste.core.OlisteConfig;
import net.oliste.core.es.EventDispatcherRecorder;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.model.EventDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class EventDispatcherTest {
  @Mock private OlisteConfig olisteConfig;

  private TestHandler testHandler;

  @BeforeEach
  void setUp() {
    testHandler = mock(TestHandler.class);
    QuarkusMock.installMockForType(testHandler, TestHandler.class);
  }

  @Test
  void eventWithoutHandlerShouldThrowException() {
    EventDispatcher eventDisp = provideInstanceOfDispatcher(true);

    var testEvennt1 = TestUnhandledEvent.builder().entityId(1L).build();
    var testAggregate1 = new TestAggregate(1L, 1L);

    Assertions.assertThatThrownBy(() -> eventDisp.applyEvent(testEvennt1, testAggregate1))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("There is no registered handler for event");

    verify(testHandler, times(0)).firstHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).secondHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).deleteHandlerMethod(any(), eq(testAggregate1));
  }

  @Test
  void handlerWithoutResolvedBeanShouldThrowException() {
    EventDispatcher eventDisp = provideInstanceOfDispatcher(false);

    var testEvennt1 = TestUnhandledEvent.builder().entityId(1L).build();
    var testAggregate1 = new TestAggregate(1L, 1L);

    Assertions.assertThatThrownBy(() -> eventDisp.applyEvent(testEvennt1, testAggregate1))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("There is no registered handler for event");

    verify(testHandler, times(0)).firstHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).secondHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).deleteHandlerMethod(any(), eq(testAggregate1));
  }

  @Test
  void handlerWithoutResolvedBeanButWithTheHandlerInEntityShouldBeInvoked() {
    EventDispatcher eventDisp = provideInstanceOfDispatcher(false);

    var testEvennt1 = Event.builder().entityId(1L).build();
    var testAggregate1 = mock(TestAggregate.class);

    eventDisp.applyEvent(testEvennt1, testAggregate1);

    verify(testHandler, times(0)).firstHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).secondHandlerMethod(any(), eq(testAggregate1));
    verify(testHandler, times(0)).deleteHandlerMethod(any(), eq(testAggregate1));

    verify(testAggregate1, times(1)).invokeEvent(testEvennt1);
  }

  @Test
  void applyEventSingleEvent() {
    EventDispatcher eventDisp = provideInstanceOfDispatcher(true);

    var testEvennt1 = TestUpdateEvent.builder().name("R1").entityId(1L).build();
    var testAggregate1 = new TestAggregate(1L, 1L);
    eventDisp.applyEvent(testEvennt1, testAggregate1);

    verify(testHandler, times(1)).firstHandlerMethod(testEvennt1, testAggregate1);
    verify(testHandler, times(1)).secondHandlerMethod(testEvennt1, testAggregate1);
    verify(testHandler, times(0)).deleteHandlerMethod(any(), eq(testAggregate1));
  }

  @Test
  void applyAllEvents() {
    EventDispatcher eventDisp = provideInstanceOfDispatcher(true);

    var testEvennt1 = TestUpdateEvent.builder().name("R1").entityId(1L).build();
    var testAggregate1 = new TestAggregate(1L, 1L);
    var deleteEvent = TestDeleteEvent.builder().name("R2").entityId(1L).build();
    eventDisp.applyEvents(List.of(testEvennt1, deleteEvent), testAggregate1);

    verify(testHandler, times(1)).firstHandlerMethod(testEvennt1, testAggregate1);
    verify(testHandler, times(1)).secondHandlerMethod(testEvennt1, testAggregate1);
    verify(testHandler, times(1)).deleteHandlerMethod(deleteEvent, testAggregate1);
  }

  private EventDispatcher provideInstanceOfDispatcher(boolean resolveBeans) {
    var eventDispatcherRecorder = new EventDispatcherRecorder();
    var eventDispatcherSupplier =
        eventDispatcherRecorder.createEventDispatcherSupplier(
            olisteConfig, List.of(TestHandler.class.getName(), TestAggregate.class.getName()));

    var eventDisp = eventDispatcherSupplier.get();
    Assertions.assertThat(eventDisp).isNotNull().isInstanceOf(EventDispatcher.class);
    if (resolveBeans) {
      eventDisp.resolveBeans();
    }
    return eventDisp;
  }

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "TestUnhandledEvent", entityName = "TestEntity")
  public static class TestUnhandledEvent extends Event {
    private final String name;
  }

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "TestUpdateEvent", entityName = "TestEntity")
  public static class TestUpdateEvent extends Event {
    private final String name;
  }

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "TestDeleteEvent", entityName = "TestEntity")
  public static class TestDeleteEvent extends Event {
    private final String name;
  }

  @Setter
  @Getter
  @AllArgsConstructor
  private static class TestAggregate implements Aggregate<Event> {
    private Long id;
    private Long version;

    @EventHandler
    public void invokeEvent(Event event) {
      version = 1000L;
    }
  }

  @ApplicationScoped
  private static class TestHandler {

    @EventHandler
    public void firstHandlerMethod(TestUpdateEvent event, TestAggregate entity) {
      System.out.println(event);
    }

    @EventHandler
    public void secondHandlerMethod(TestUpdateEvent event, TestAggregate entity) {
      System.out.println(event);
    }

    @EventHandler
    public void deleteHandlerMethod(TestDeleteEvent event, TestAggregate entity) {
      System.out.println(event);
    }
  }
}
