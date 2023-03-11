package net.oliste.core.es.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.model.EventTrace;
import net.oliste.core.es.persistance.PersistentEvent;

@RequiredArgsConstructor
@Unremovable
public class EventMapper {

  private final ObjectMapper objectMapper;
  private Map<Class<?>, EventDescripton> eventDescriptionMap = new HashMap<>();
  private Map<String, Map<String, Class<?>>> eventDescriptionToClassMap = new HashMap<>();

  @Getter
  @AllArgsConstructor(staticName = "of")
  public static class EventDescripton {
    private final Class<?> eventClass;
    private final String eventName;
    private final String entityName;
  }

  public void initialize(
      Map<Class<?>, EventDescripton> eventDescriptionMap,
      Map<String, Map<String, Class<?>>> eventDescriptionToClassMap) {
    this.eventDescriptionMap = eventDescriptionMap;
    this.eventDescriptionToClassMap = eventDescriptionToClassMap;
  }

  public <T extends Event, S extends PersistentEvent> S toPersistentEntity(
      @NonNull T event, S persistentEvent) {
    mapClassToEventAndEntityName(
        event,
        (eventName, entityName) -> {
          persistentEvent.setEventName(eventName);
          persistentEvent.setEntityName(entityName);
          return null;
        });

    persistentEvent.setEntityId(event.getEntityId());
    EventTrace trace = event.getTrace();
    if (trace != null) {
      persistentEvent.setTraceId(trace.getTraceId());
      persistentEvent.setSpanId(trace.getSpanId());
      persistentEvent.setParentId(trace.getParentId());
    }
    persistentEvent.setCreatedAt(event.getCreatedAt());
    addSerializedEventData(event, persistentEvent);

    return persistentEvent;
  }

  private <T extends Event, S extends PersistentEvent> void addSerializedEventData(
      T event, S persistentEvent) {
    if (!objectMapper.canSerialize(event.getClass())) {
      throw new RuntimeException("Cannot serialize class " + event.getClass().getCanonicalName());
    }
    try {
      String json = objectMapper.writeValueAsString(event);
      persistentEvent.setEventData(json);
    } catch (JsonProcessingException exception) {
      throw new RuntimeException("Error during event serialization", exception);
    }
  }

  public <T extends Event, S> S mapClassToEventAndEntityName(
      T event, BiFunction<String, String, S> mapping) {
    final Class<? extends Event> eventClass = event.getClass();
    final EventDescripton eventDescripton = eventDescriptionMap.getOrDefault(eventClass, null);
    if (eventDescripton == null) {
      throw new RuntimeException(
          "Couldn't find event description for class " + eventClass.getCanonicalName());
    }
    return mapping.apply(eventDescripton.getEventName(), eventDescripton.getEntityName());
  }

  <T extends Event> Class<T> mapEventAndEntityNameToClass(String eventName, String entityName) {
    final Map<String, Class<?>> map = eventDescriptionToClassMap.get(eventName);
    return map == null ? null : (Class<T>) map.get(entityName);
  }

  public <T extends Event> T toEvent(PersistentEvent persistentEvent) {
    Class<T> eventClass =
        mapEventAndEntityNameToClass(
            persistentEvent.getEventName(), persistentEvent.getEntityName());
    if (eventClass == null) {
      throw new RuntimeException(
          "Event class not found for event name ("
              + persistentEvent.getEventName()
              + ") and entity name ("
              + persistentEvent.getEntityName()
              + ")");
    }
    try {
      return objectMapper.readValue(persistentEvent.getEventData(), eventClass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Error during deserialization of class " + eventClass.getCanonicalName(), e);
    }
  }
}
