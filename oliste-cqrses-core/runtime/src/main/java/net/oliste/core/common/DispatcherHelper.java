package net.oliste.core.common;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DispatcherHelper {
  public static String enumerateClasses(Collection<Class<?>> classes) {
    return classes.stream()
        .map(cls -> cls.getCanonicalName())
        .collect(Collectors.joining(",", "[", "]"));
  }

  public String getExceptionMethodInfo(Method method) {
    return method.getName() + " in " + method.getDeclaringClass().getCanonicalName();
  }
}
