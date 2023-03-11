package net.oliste.core.deployment.cqrs;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.cqrs.CommandDispatcherRecorder;
import net.oliste.core.cqrs.QueriesDispatcherRecorder;
import net.oliste.core.cqrs.command.CommandDispatcher;
import net.oliste.core.cqrs.command.CommandHandler;
import net.oliste.core.cqrs.query.QueryDispatcher;
import net.oliste.core.cqrs.query.QueryHandler;
import net.oliste.core.deployment.core.AnnotationScanner;

public class CqrsProcessor {
  private static final Logger LOGGER = Logger.getLogger(CqrsProcessor.class.getCanonicalName());
  private static final String OLISTE_COMMAND = "oliste-cqrs";
  private static final AnnotationScanner SCANNER = new AnnotationScanner();

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(OLISTE_COMMAND);
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static final class CommandsBuildItem extends MultiBuildItem {
    private List<String> classes;

    CommandsBuildItem() {
      super();
    }
  }

  @BuildStep
  void buildCommands(
      OlisteConfig config,
      BuildProducer<ReflectiveClassBuildItem> reflectionProvider,
      BuildProducer<CommandsBuildItem> commandsBuildItemBuildProducer) {
    if (!config.cqrs.enabled) {
      LOGGER.info("Commands processor is disabled due to confituration");
    }
    final List<ReflectiveClassBuildItem> list = new ArrayList<>();

    final List<Class<?>> classes =
        SCANNER.findAllClassesWithAnnotatedMethods(config.packageName, CommandHandler.class);

    classes.forEach(
        cls ->
            reflectionProvider.produce(
                new ReflectiveClassBuildItem(true, true, cls.getCanonicalName())));
    LOGGER.info("Commands classes : " + DispatcherHelper.enumerateClasses(classes));

    List<String> names = SCANNER.getNamesList(classes);

    commandsBuildItemBuildProducer.produce(CommandsBuildItem.builder().classes(names).build());
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static final class QueriesBuildItem extends MultiBuildItem {
    private List<String> classes;

    QueriesBuildItem() {
      super();
    }
  }

  @BuildStep
  void buildQueries(
      OlisteConfig config,
      BuildProducer<ReflectiveClassBuildItem> reflectionProvider,
      BuildProducer<QueriesBuildItem> queriesBuildItemBuildProducer) {
    if (!config.cqrs.enabled) {
      LOGGER.info("Queries processor is disabled due to confituration");
    }
    final List<ReflectiveClassBuildItem> list = new ArrayList<>();

    final List<Class<?>> classes =
        SCANNER.findAllClassesWithAnnotatedMethods(config.packageName, QueryHandler.class);

    classes.forEach(
        cls ->
            reflectionProvider.produce(
                new ReflectiveClassBuildItem(true, true, cls.getCanonicalName())));
    LOGGER.info("Queries classes : " + DispatcherHelper.enumerateClasses(classes));

    List<String> names = SCANNER.getNamesList(classes);

    queriesBuildItemBuildProducer.produce(QueriesBuildItem.builder().classes(names).build());
  }

  @BuildStep
  @Record(STATIC_INIT)
  SyntheticBeanBuildItem commandDispatcherInit(
      CommandDispatcherRecorder recorder,
      List<CommandsBuildItem> commandsBuildItems,
      OlisteConfig config) {

    List<String> names =
        commandsBuildItems.stream()
            .flatMap(item -> item.getClasses().stream())
            .collect(Collectors.toList());

    return SyntheticBeanBuildItem.configure(CommandDispatcher.class)
        .scope(Singleton.class)
        .supplier(recorder.createCommandDispatcherSupplier(config, names))
        .done();
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  @Consume(SyntheticBeanBuildItem.class)
  void initializeCommandsDispatcherBean(CommandDispatcherRecorder recorder) {
    recorder.runtimeInit();
  }

  @BuildStep
  @Record(STATIC_INIT)
  SyntheticBeanBuildItem queriesDispatcherInit(
      QueriesDispatcherRecorder recorder,
      List<QueriesBuildItem> queriesBuildItems,
      OlisteConfig config) {

    List<String> names =
        queriesBuildItems.stream()
            .flatMap(item -> item.getClasses().stream())
            .collect(Collectors.toList());

    return SyntheticBeanBuildItem.configure(QueryDispatcher.class)
        .scope(Singleton.class)
        .supplier(recorder.queriesDispatcherSupplier(config, names))
        .done();
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  @Consume(SyntheticBeanBuildItem.class)
  void initializeQueriesDispatcherBean(QueriesDispatcherRecorder recorder) {
    recorder.runtimeInit();
  }
}
