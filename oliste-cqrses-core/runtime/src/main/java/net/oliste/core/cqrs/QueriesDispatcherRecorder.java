package net.oliste.core.cqrs;

import io.quarkus.runtime.annotations.Recorder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import lombok.Value;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.AnnotationScanner;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.cqrs.query.Query;
import net.oliste.core.cqrs.query.QueryDispatcher;
import net.oliste.core.cqrs.query.QueryHandler;

@Recorder
public class QueriesDispatcherRecorder {
  public Supplier<QueryDispatcher> queriesDispatcherSupplier(
      OlisteConfig config, List<String> classesN) {

    return () -> {
      final DispatcherHelper helper = CDI.current().select(DispatcherHelper.class).get();
      final QueryDispatcher queryDispatcher = new QueryDispatcher(helper);
      final List<Class<?>> classes = AnnotationScanner.resolveClassesForNames(classesN);

      final Map<Class<?>, List<Method>> handlersMap =
          AnnotationScanner.findAnnotatedMethodsOfClassesGroupByFirstType(
              classes, QueryHandler.class, Query.class);

      @Value
      class ClassMethodMap {
        Class<?> methodClass;
        Method method;
      }

      final Map<Class<?>, Method> handlers =
          handlersMap.entrySet().stream()
              .map(
                  entry -> {
                    final Class<?> key = entry.getKey();
                    final Method method = getQueryHandlerMethod(entry);
                    return new ClassMethodMap(key, method);
                  })
              .collect(Collectors.toMap(ClassMethodMap::getMethodClass, ClassMethodMap::getMethod));

      queryDispatcher.initialize(handlers, classes);
      return queryDispatcher;
    };
  }

  public void runtimeInit() {
    final QueryDispatcher queryDispatcher = CDI.current().select(QueryDispatcher.class).get();
    queryDispatcher.resolveBeans();
  }

  private Method getQueryHandlerMethod(Map.Entry<Class<?>, List<Method>> entry) {
    final List<Method> methods = entry.getValue();
    if (methods.size() > 1) {
      throw new RuntimeException("Too many handlers for query " + entry.getKey());
    }
    Method method = methods.stream().findFirst().get();
    return method;
  }
}
