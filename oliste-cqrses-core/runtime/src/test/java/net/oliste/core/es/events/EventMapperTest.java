package net.oliste.core.es.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.persistance.PersistentEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventMapperTest {

  @Mock private ObjectMapper objectMapper;
  private EventMapper eventMapper;

  @BeforeEach
  void setUp() {
    eventMapper = new EventMapper(objectMapper);
  }

  @Getter
  @Setter
  @SuperBuilder
  @JsonSerialize
  static class TestEvent extends Event {}

  @Test
  void shouldSetProprtiesOfPersistentEntity() throws JsonProcessingException {
    Map<Class<?>, EventMapper.EventDescripton> eventDesc =
        Map.of(
            TestEvent.class,
            EventMapper.EventDescripton.of(TestEvent.class, "TestEvent", "TestEntity"));
    eventMapper.initialize(eventDesc, Collections.emptyMap());

    var event = TestEvent.builder().entityId(12L).build();
    var persistentEvent = mock(PersistentEvent.class);

    when(objectMapper.canSerialize(event.getClass())).thenReturn(true);
    var result = eventMapper.toPersistentEntity(event, persistentEvent);

    Assertions.assertThat(result).isSameAs(persistentEvent);

    verify(objectMapper, times(1)).writeValueAsString(any(TestEvent.class));

    verify(persistentEvent, times(1)).setEntityName(any());
    verify(persistentEvent, times(1)).setEntityId(eq(12L));
    verify(persistentEvent, times(1)).setEventName(any());
    verify(persistentEvent, times(1)).setCreatedAt(any());

    verify(persistentEvent, times(1)).setEventData(any());
  }

  @Test
  void shouldThrowAnExceptionWhenEventClassIsNotSerializable() {
    Map<Class<?>, EventMapper.EventDescripton> eventDesc =
        Map.of(
            TestEvent.class,
            EventMapper.EventDescripton.of(TestEvent.class, "TestEvent", "TestEntity"));
    eventMapper.initialize(eventDesc, Collections.emptyMap());

    var event = TestEvent.builder().build();
    var persistentEvent = mock(PersistentEvent.class);

    when(objectMapper.canSerialize(event.getClass())).thenReturn(false);
    Assertions.assertThatThrownBy(() -> eventMapper.toPersistentEntity(event, persistentEvent))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Cannot serialize class");
  }

  @Test
  void shouldThrowAnExceptionWhenSerializationIssuesOccurs() throws JsonProcessingException {
    Map<Class<?>, EventMapper.EventDescripton> eventDesc =
        Map.of(
            TestEvent.class,
            EventMapper.EventDescripton.of(TestEvent.class, "TestEvent", "TestEntity"));
    eventMapper.initialize(eventDesc, Collections.emptyMap());

    var event = TestEvent.builder().build();
    var persistentEvent = mock(PersistentEvent.class);

    when(objectMapper.canSerialize(event.getClass())).thenReturn(true);
    doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(event);

    Assertions.assertThatThrownBy(() -> eventMapper.toPersistentEntity(event, persistentEvent))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Error during event serialization")
        .hasCauseInstanceOf(JsonProcessingException.class);
  }

  @Test
  void shouldMapClassToEventAndEntityName() {
    Map<Class<?>, EventMapper.EventDescripton> eventDesc =
        Map.of(Event.class, EventMapper.EventDescripton.of(Event.class, "TestEvent", "TestEntity"));
    eventMapper.initialize(eventDesc, Collections.emptyMap());

    var event = Event.builder().build();
    var result =
        eventMapper.<Event, String>mapClassToEventAndEntityName(
            event, (eventName, entityName) -> eventName + ":" + entityName);

    Assertions.assertThat(result).isEqualTo("TestEvent:TestEntity");
  }

  @Test
  void shouldMapEventAndEntityNameToClass() {
    Map<String, Map<String, Class<?>>> eventDesc =
        Map.of("TestEvent", Map.of("TestEntity", Event.class));
    eventMapper.initialize(Collections.emptyMap(), eventDesc);

    var result = eventMapper.mapEventAndEntityNameToClass("TestEvent", "TestEntity");

    Assertions.assertThat(result).isSameAs(Event.class);
  }

  @Test
  void shouldDeserializeContentAndReturnAnEventFromPersistentEvent()
      throws JsonProcessingException {
    Map<String, Map<String, Class<?>>> eventDesc =
        Map.of("TestEvent", Map.of("TestEntity", Event.class));
    eventMapper.initialize(Collections.emptyMap(), eventDesc);

    var persistentEvent = mock(PersistentEvent.class);
    when(persistentEvent.getEventName()).thenReturn("TestEvent");
    when(persistentEvent.getEntityName()).thenReturn("TestEntity");
    when(persistentEvent.getEventData()).thenReturn("{}");

    var event = mock(Event.class);
    when(objectMapper.readValue("{}", Event.class)).thenReturn(event);

    var result = eventMapper.toEvent(persistentEvent);
    Assertions.assertThat(result).isSameAs(event);

    verify(objectMapper, times(1)).readValue("{}", Event.class);
  }

  @Test
  void shouldThrowExceptionWhenThereIsNoMappingOfEventNameOrEntityName() {
    Map<String, Map<String, Class<?>>> eventDesc =
        Map.of("TestEvent", Map.of("TestEntity2", Event.class));
    eventMapper.initialize(Collections.emptyMap(), eventDesc);

    var persistentEvent = mock(PersistentEvent.class);
    when(persistentEvent.getEventName()).thenReturn("TestEvent");
    when(persistentEvent.getEntityName()).thenReturn("TestEntity");

    Assertions.assertThatThrownBy(() -> eventMapper.toEvent(persistentEvent))
        .hasMessageContaining("Event class not found for event name");

    eventDesc = Map.of("TestEvent2", Map.of("TestEntity", Event.class));
    eventMapper.initialize(Collections.emptyMap(), eventDesc);

    Assertions.assertThatThrownBy(() -> eventMapper.toEvent(persistentEvent))
        .hasMessageContaining("Event class not found for event name");
  }

  @Test
  void shouldThrowAnErrorWithJsonProcessingExceptionAsTheCauseWhenDeserializationIssueOccurs()
      throws JsonProcessingException {
    Map<String, Map<String, Class<?>>> eventDesc =
        Map.of("TestEvent", Map.of("TestEntity", Event.class));
    eventMapper.initialize(Collections.emptyMap(), eventDesc);

    var persistentEvent = mock(PersistentEvent.class);
    when(persistentEvent.getEventName()).thenReturn("TestEvent");
    when(persistentEvent.getEntityName()).thenReturn("TestEntity");
    when(persistentEvent.getEventData()).thenReturn("{}");

    doThrow(JsonProcessingException.class).when(objectMapper).readValue(eq("{}"), eq(Event.class));

    Assertions.assertThatThrownBy(() -> eventMapper.toEvent(persistentEvent))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(JsonProcessingException.class);
  }
}
