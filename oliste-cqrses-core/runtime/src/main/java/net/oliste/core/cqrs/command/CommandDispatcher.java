package net.oliste.core.cqrs.command;

import io.smallrye.mutiny.Multi;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.common.DispatcherHelper;

public class CommandDispatcher {
  private Map<Class<?>, List<Method>> handlers = new HashMap<>();
  private List<Class<?>> handlerClasses = new ArrayList<>();
  private Map<Class<?>, Object> handlersObjects = new HashMap<>();

  private DispatcherHelper dispatcherHelper;

  public CommandDispatcher(DispatcherHelper dispatcherHelper) {
    this.dispatcherHelper = dispatcherHelper;
  }

  public <T extends Command, S> Multi<S> execute(T command) {
    Class<? extends Command> commandClass = command.getClass();
    List<Method> methods = handlers.get(commandClass);
    Multi<S> multi = Multi.createFrom().empty();
    if (methods == null || methods.isEmpty()) {
      throw new RuntimeException(
          "There is no registered handler for command " + commandClass.getCanonicalName());
    }
    for (Method method : methods) {
      Object obj = handlersObjects.get(method.getDeclaringClass());
      if (obj == null) {
        throw new RuntimeException(
            "Handling object not registered for "
                + dispatcherHelper.getExceptionMethodInfo(method));
      }
      try {
        Object result = method.invoke(obj, command);
        if (result == null || (!(result instanceof Multi))) {
          throw new RuntimeException(
              "Invalid response type for method "
                  + dispatcherHelper.getExceptionMethodInfo(method));
        }
        Multi<S> resultMulti = Multi.class.cast(result);
        multi = Multi.createBy().concatenating().streams(multi, resultMulti);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(
            "Invocation error " + dispatcherHelper.getExceptionMethodInfo(method), e);
      }
    }

    return multi;
  }

  public void initialize(Map<Class<?>, List<Method>> handlers, List<Class<?>> handlerClasses) {
    this.handlers = handlers;
    this.handlerClasses = handlerClasses;
  }

  public void resolveBeans() {
    handlersObjects =
        handlerClasses.stream()
            .filter(handlerClass -> !CommandHandler.class.equals(handlerClass))
            .collect(
                Collectors.toMap(
                    handlerClass -> handlerClass,
                    handlerClass -> CDI.current().select(handlerClass).get(),
                    (v1, v2) -> v1));
  }
}
