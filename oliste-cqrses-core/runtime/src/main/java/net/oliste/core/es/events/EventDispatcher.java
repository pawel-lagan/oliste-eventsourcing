package net.oliste.core.es.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.es.model.Aggregate;
import net.oliste.core.es.model.Event;

public class EventDispatcher {
  private Map<Class<?>, List<Method>> handlers = new HashMap<>();
  private List<Class<?>> handlerClasses = new ArrayList<>();
  private Map<Class<?>, Object> handlersObjects = new HashMap<>();

  private final DispatcherHelper dispatcherHelper;

  public EventDispatcher(DispatcherHelper dispatcherHelper) {
    this.dispatcherHelper = dispatcherHelper;
  }

  public <T extends Event, S, Q extends Aggregate> void applyEvent(T event, Q entity) {
    Class<? extends Event> eventClass = event.getClass();
    List<Method> methods = handlers.get(eventClass);
    boolean foreginEventHandler = false;
    if (methods == null || methods.isEmpty()) {
      throw new RuntimeException(
          "There is no registered handler for event " + eventClass.getCanonicalName());
    }

    for (Method method : methods) {
      if (entity == null) {
        throw new RuntimeException("Entity parameter is null");
      }

      foreginEventHandler = handlersObjects.containsKey(method.getDeclaringClass());
      try {
        if (foreginEventHandler) {
          method.invoke(handlersObjects.get(method.getDeclaringClass()), event, entity);
        } else {
          method.invoke(entity, event);
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(
            "Invocation error " + dispatcherHelper.getExceptionMethodInfo(method), e);
      }
    }
  }

  public <T extends Event, S, Q extends Aggregate> void applyEvents(
      Collection<T> events, Q entity) {
    events.forEach(event -> applyEvent(event, entity));
  }

  public void initialize(Map<Class<?>, List<Method>> handlers, List<Class<?>> handlerClasses) {
    this.handlers = handlers;
    this.handlerClasses = handlerClasses;
  }

  public void resolveBeans() {
    handlersObjects =
        handlerClasses.stream()
            .filter(handlerClass -> !Aggregate.class.isAssignableFrom(handlerClass))
            .filter(handlerClass -> !Event.class.isAssignableFrom(handlerClass))
            .collect(
                Collectors.toMap(
                    handlerClass -> handlerClass,
                    handlerClass -> CDI.current().select(handlerClass).get(),
                    (v1, v2) -> v1));
  }
}
