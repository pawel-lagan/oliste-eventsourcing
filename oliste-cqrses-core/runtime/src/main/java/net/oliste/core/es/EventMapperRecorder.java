package net.oliste.core.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.Recorder;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.AnnotationScanner;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.EventDescriptor;

@Recorder
public class EventMapperRecorder {

  public Supplier<EventMapper> createEventDispatcherSupplier(
      OlisteConfig config, List<String> classesN) {
    return () -> {
      final List<Class<?>> classes = AnnotationScanner.resolveClassesForNames(classesN);

      Map<Class<?>, EventMapper.EventDescripton> eventDescriptonMap =
          classes.stream()
              .map(
                  (cls) -> {
                    EventDescriptor descriptor = cls.getAnnotation(EventDescriptor.class);
                    return EventMapper.EventDescripton.of(
                        cls, descriptor.eventName(), descriptor.entityName());
                  })
              .collect(
                  Collectors.toMap(
                      EventMapper.EventDescripton::getEventClass, descripton -> descripton));

      Map<String, Map<String, Class<?>>> map =
          eventDescriptonMap.values().stream()
              .collect(
                  Collectors.groupingBy(
                      EventMapper.EventDescripton::getEventName,
                      Collectors.toMap(
                          EventMapper.EventDescripton::getEntityName,
                          EventMapper.EventDescripton::getEventClass)));

      ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
      final EventMapper eventMapper = new EventMapper(objectMapper);
      eventMapper.initialize(eventDescriptonMap, map);
      return eventMapper;
    };
  }
}
