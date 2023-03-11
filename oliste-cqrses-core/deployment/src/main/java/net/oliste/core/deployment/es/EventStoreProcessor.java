package net.oliste.core.deployment.es;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.deployment.core.AnnotationScanner;
import net.oliste.core.es.EventDispatcherRecorder;
import net.oliste.core.es.EventMapperRecorder;
import net.oliste.core.es.events.EventDispatcher;
import net.oliste.core.es.events.EventHandler;
import net.oliste.core.es.events.EventMapper;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.model.EventDescriptor;

public class EventStoreProcessor {
  private static final Logger LOGGER =
      Logger.getLogger(EventStoreProcessor.class.getCanonicalName());
  private static final String OLISTE_EVENT_STORE = "oliste-event-store";
  private static final AnnotationScanner SCANNER = new AnnotationScanner();

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(OLISTE_EVENT_STORE);
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static final class EventHandlersBuildItem extends MultiBuildItem {
    private List<String> classes;

    EventHandlersBuildItem() {
      super();
    }
  }

  @BuildStep
  void buildEventDispatcher(
      OlisteConfig config,
      BuildProducer<ReflectiveClassBuildItem> reflectionProvider,
      BuildProducer<EventHandlersBuildItem> eventHandlersBuildItemBuildProducer) {
    if (!config.es.enabled) {
      LOGGER.info("Event store processor is disabled due to confituration");
      return;
    }
    final List<Class<?>> classes =
        SCANNER.findAllClassesWithAnnotatedMethods(config.packageName, EventHandler.class);
    classes.forEach(
        cls ->
            reflectionProvider.produce(
                new ReflectiveClassBuildItem(true, true, cls.getCanonicalName())));
    LOGGER.info("Classes with event handlers : " + DispatcherHelper.enumerateClasses(classes));

    List<String> names = SCANNER.getNamesList(classes);

    EventHandlersBuildItem item = EventHandlersBuildItem.builder().classes(names).build();

    eventHandlersBuildItemBuildProducer.produce(item);
  }

  @BuildStep
  @Record(STATIC_INIT)
  SyntheticBeanBuildItem eventDispatcherStaticInit(
      EventDispatcherRecorder recorder,
      List<EventHandlersBuildItem> eventHandlerScanBuildItem,
      OlisteConfig config) {
    List<String> names =
        eventHandlerScanBuildItem.stream()
            .flatMap(item -> item.getClasses().stream())
            .collect(Collectors.toList());

    return SyntheticBeanBuildItem.configure(EventDispatcher.class)
        .scope(Singleton.class)
        .supplier(recorder.createEventDispatcherSupplier(config, names))
        .done();
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  @Consume(SyntheticBeanBuildItem.class)
  void initializeEventDispatcherBean(EventDispatcherRecorder recorder) {
    recorder.runtimeInit();
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static final class EventDescriptorsBuildItem extends MultiBuildItem {
    private List<String> classes;

    EventDescriptorsBuildItem() {
      super();
    }
  }

  @BuildStep
  void buildEventMapper(
      OlisteConfig config,
      BuildProducer<ReflectiveClassBuildItem> reflectionProvider,
      BuildProducer<EventDescriptorsBuildItem> eventDescriptorsBuildItemBuildProducer) {
    if (!config.es.enabled) {
      LOGGER.info("Event store processor is disabled due to confituration");
    }
    final Set<Class<?>> classes =
        SCANNER
            .findAllAnnotatedClasses(config.packageName, EventDescriptor.class, Event.class)
            .keySet();

    classes.forEach(
        cls ->
            reflectionProvider.produce(
                new ReflectiveClassBuildItem(true, true, cls.getCanonicalName())));
    LOGGER.info("Events classes : " + DispatcherHelper.enumerateClasses(classes));

    List<String> names = SCANNER.getNamesList(classes);

    EventDescriptorsBuildItem item = EventDescriptorsBuildItem.builder().classes(names).build();

    eventDescriptorsBuildItemBuildProducer.produce(item);
  }

  @BuildStep
  @Record(STATIC_INIT)
  SyntheticBeanBuildItem eventMapper(
      EventMapperRecorder recorder,
      List<EventDescriptorsBuildItem> eventDescriptorsBuildItem,
      OlisteConfig config) {

    List<String> names =
        eventDescriptorsBuildItem.stream()
            .flatMap(item -> item.getClasses().stream())
            .collect(Collectors.toList());

    return SyntheticBeanBuildItem.configure(EventMapper.class)
        .scope(Singleton.class)
        .supplier(recorder.createEventDispatcherSupplier(config, names))
        .done();
  }
}
