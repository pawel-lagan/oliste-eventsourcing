package net.oliste.core.cqrs.query;

import io.smallrye.mutiny.Uni;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.common.DispatcherHelper;

public class QueryDispatcher {
  private Map<Class<?>, Method> queries;
  private List<Class<?>> queriesClasses;
  private Map<Class<?>, Object> queriesObjects = new HashMap<>();

  private DispatcherHelper dispatcherHelper;

  public QueryDispatcher(DispatcherHelper dispatcherHelper) {
    this.dispatcherHelper = dispatcherHelper;
  }

  public <T, S extends Query> Uni<T> query(S query) {
    Class<? extends Query> queryClass = query.getClass();
    Method method = queries.get(queryClass);
    if (method == null) {
      throw new RuntimeException("No query handler for query " + queryClass.getCanonicalName());
    }
    Object obj = queriesObjects.get(method.getDeclaringClass());
    if (obj == null) {
      throw new RuntimeException(
          "Handling object not registered for " + dispatcherHelper.getExceptionMethodInfo(method));
    }

    try {
      Object result = method.invoke(obj, query);
      return Uni.class.cast(result);
    } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(
          "Handling query registered for " + dispatcherHelper.getExceptionMethodInfo(method), e);
    }
  }

  public void initialize(Map<Class<?>, Method> queries, List<Class<?>> queriesClasses) {
    this.queries = queries;
    this.queriesClasses = queriesClasses;
  }

  public void resolveBeans() {
    queriesObjects =
        queriesClasses.stream()
            .filter(handlerClass -> !QueryHandler.class.equals(handlerClass))
            .collect(
                Collectors.toMap(
                    handlerClass -> handlerClass,
                    handlerClass -> CDI.current().select(handlerClass).get(),
                    (v1, v2) -> v1));
  }
}
