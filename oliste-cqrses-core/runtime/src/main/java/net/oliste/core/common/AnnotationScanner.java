package net.oliste.core.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationScanner {

  public static List<Method> findAnnotatedMethodsOfClass(
      Class<?> clazz, Class<? extends Annotation> annotationClazz, Class<?> typeClass) {
    return Arrays.stream(clazz.getDeclaredMethods())
        .filter(
            method ->
                method.isAnnotationPresent(annotationClazz) && method.getParameters().length > 0)
        .filter(
            method ->
                Arrays.stream(method.getParameters())
                    .filter(parameter -> typeClass.isAssignableFrom(parameter.getType()))
                    .map(a -> true)
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new RuntimeException(
                                "Command param not found in handler "
                                    + getExceptionMethodInfo(method))))
        .distinct()
        .collect(Collectors.toList());
  }

  public static Map<Class<?>, List<Method>> findAnnotatedMethodsOfClassesGroupByFirstType(
      Collection<Class<?>> classCollection,
      Class<? extends Annotation> annotationClazz,
      Class<?> typeClass) {
    return classCollection.stream()
        .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
        .filter(
            method ->
                method.isAnnotationPresent(annotationClazz) && method.getParameters().length > 0)
        .collect(
            Collectors.groupingBy(
                method ->
                    Arrays.stream(method.getParameters())
                        .filter(parameter -> typeClass.isAssignableFrom(parameter.getType()))
                        .map(parameter -> parameter.getType())
                        .findFirst()
                        .orElseThrow(
                            () ->
                                new RuntimeException(
                                    "Command param not found in handler "
                                        + getExceptionMethodInfo(method))),
                Collectors.toList()));
  }

  private static String getExceptionMethodInfo(Method method) {
    return method.getName() + " in " + method.getDeclaringClass().getCanonicalName();
  }

  public static List<Class<?>> resolveClassesForNames(List<String> classesN) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    List<Class<?>> classes =
        classesN.stream()
            .map(
                name -> {
                  Class<?> cls = null;
                  try {
                    cls = Class.forName(name, true, classLoader);
                  } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Couldn't load class " + name, e);
                  }
                  return cls;
                })
            .distinct()
            .collect(Collectors.toList());
    return classes;
  }
}
