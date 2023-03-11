package net.oliste.core.cqrs;

import io.quarkus.runtime.annotations.Recorder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.enterprise.inject.spi.CDI;
import net.oliste.core.OlisteConfig;
import net.oliste.core.common.AnnotationScanner;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.cqrs.command.Command;
import net.oliste.core.cqrs.command.CommandDispatcher;
import net.oliste.core.cqrs.command.CommandHandler;

@Recorder
public class CommandDispatcherRecorder {
  public Supplier<CommandDispatcher> createCommandDispatcherSupplier(
      OlisteConfig config, List<String> classesN) {
    return () -> {
      final List<Class<?>> classes = AnnotationScanner.resolveClassesForNames(classesN);

      final Map<Class<?>, List<Method>> handlersMap =
          AnnotationScanner.findAnnotatedMethodsOfClassesGroupByFirstType(
              classes, CommandHandler.class, Command.class);

      final DispatcherHelper helper = CDI.current().select(DispatcherHelper.class).get();
      final CommandDispatcher commandDispatcher = new CommandDispatcher(helper);
      commandDispatcher.initialize(handlersMap, classes);
      return commandDispatcher;
    };
  }

  public void runtimeInit() {
    final CommandDispatcher commandDispatcher = CDI.current().select(CommandDispatcher.class).get();
    commandDispatcher.resolveBeans();
  }
}
