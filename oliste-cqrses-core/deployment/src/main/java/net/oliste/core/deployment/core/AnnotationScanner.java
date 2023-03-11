package net.oliste.core.deployment.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class AnnotationScanner {

  public Map<Class<?>, List<Method>> findAllAnnotatedMethods(
      String packageName, Class<? extends Annotation> annotationClazz, Class<?> typeClass) {
    return findAllClassesUsingReflectionsLibrary(packageName).stream()
        .flatMap(clazz -> Arrays.stream(clazz.getMethods()))
        .filter(method -> method.isAnnotationPresent(annotationClazz))
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

  public List<Class<?>> findAllClassesWithAnnotatedMethods(
      String packageName, Class<? extends Annotation> annotationClazz) {
    return findAllClassesUsingReflectionsLibrary(packageName).stream()
        .flatMap(clazz -> Arrays.stream(clazz.getMethods()))
        .filter(method -> method.isAnnotationPresent(annotationClazz))
        .map(method -> method.getDeclaringClass())
        .distinct()
        .collect(Collectors.toList());
  }

  public static List<String> getNamesList(Collection<Class<?>> classes) {
    return classes.stream().map(cls -> cls.getName()).collect(Collectors.toList());
  }

  public Map<Class<?>, Annotation> findAllAnnotatedClasses(
      String packageName, Class<? extends Annotation> annotationClazz, Class<?> typeClass) {
    return findAllClassesUsingReflectionsLibrary(packageName).stream()
        .filter(clazz -> clazz.isAnnotationPresent(annotationClazz))
        .filter(clazz -> typeClass.isAssignableFrom(clazz))
        .collect(
            Collectors.toMap(
                v1 -> v1,
                v1 -> Arrays.stream(v1.getAnnotationsByType(annotationClazz)).findFirst().get()));
  }

  private String getExceptionMethodInfo(Method method) {
    return method.getName() + " in " + method.getDeclaringClass().getName();
  }

  private Set<Class<?>> findAllClassesUsingReflectionsLibrary(String packageName) {
    Reflections reflections = new Reflections(packageName, new SubTypesScanner(false));
    return reflections.getSubTypesOf(Object.class).stream().collect(Collectors.toSet());
  }
}
