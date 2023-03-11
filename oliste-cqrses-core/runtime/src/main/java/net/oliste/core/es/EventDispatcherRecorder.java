package net.oliste.core.es;

import io.quarkus.runtime.annotations.Recorder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.AnnotationScanner;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventHandler;
import net.oliste.core.es.model.Event;

@Recorder
public class EventDispatcherRecorder {

  public Supplier<EventDispatcher> createEventDispatcherSupplier(
      OlisteConfig config, List<String> classesN) {
    return () -> {
      final List<Class<?>> classes = AnnotationScanner.resolveClassesForNames(classesN);

      final Map<Class<?>, List<Method>> handlersMap =
          AnnotationScanner.findAnnotatedMethodsOfClassesGroupByFirstType(
              classes, EventHandler.class, Event.class);

      final DispatcherHelper helper = CDI.current().select(DispatcherHelper.class).get();
      final EventDispatcher eventDispatcher = new EventDispatcher(helper);

      eventDispatcher.initialize(handlersMap, classes);
      return eventDispatcher;
    };
  }

  public void runtimeInit() {
    final EventDispatcher eventDispatcher = CDI.current().select(EventDispatcher.class).get();
    eventDispatcher.resolveBeans();
  }
}
