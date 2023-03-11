package net.oliste.core.cqrs.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import net.oliste.core.OlisteConfig;
import net.oliste.core.cqrs.CommandDispatcherRecorder;
import net.oliste.core.es.model.Event;
import net.oliste.core.es.model.EventDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

  @Mock private OlisteConfig olisteConfig;

  private TestCommandHandlers testHandler;

  @BeforeEach
  void setUp() {
    testHandler = mock(TestCommandHandlers.class);
    QuarkusMock.installMockForType(testHandler, TestCommandHandlers.class);
  }

  @Test
  void exceptionShouldBeThrownWhenThereIsNoCommandHandlerRegistered() {
    CommandDispatcher commandDispatcher = provideInstanceOfDispatcher(true);

    var unhandledCommand = UnhandledCommand.builder().build();

    Assertions.assertThatThrownBy(() -> commandDispatcher.execute(unhandledCommand))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("There is no registered handler for command ");

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(0)).secondHandlerMethod(any());
  }

  @Test
  void exceptionShouldBeThrownWhenThereIsNoBeanForObjectHandler() {
    CommandDispatcher commandDispatcher = provideInstanceOfDispatcher(false);

    var test1Command = Test1Command.builder().build();

    Assertions.assertThatThrownBy(() -> commandDispatcher.execute(test1Command))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Handling object not registered for ");

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(0)).secondHandlerMethod(any());
  }

  @Test
  void firstTestQueryShouldBeHandledProperly() {
    CommandDispatcher commandDispatcher = provideInstanceOfDispatcher(true);

    var test1Command = Test1Command.builder().build();
    commandDispatcher.<Test1Command, TestEvent>execute(test1Command);

    verify(testHandler, times(1)).firstHandlerMethod(test1Command);
    verify(testHandler, times(0)).secondHandlerMethod(any());
    verify(testHandler, times(1)).thirdHandlerMethod(test1Command);
  }

  @Test
  void firstTestQueryShouldHaveConcatenatedResultFromAllHandlers() {
    CommandDispatcher commandDispatcher = provideInstanceOfDispatcher(true);

    var test1Command = Test1Command.builder().build();
    when(testHandler.firstHandlerMethod(test1Command))
        .thenReturn(Multi.createFrom().item(TestEvent.builder().name("q1").build()));
    when(testHandler.thirdHandlerMethod(test1Command))
        .thenReturn(Multi.createFrom().item(TestEvent.builder().name("q1b").build()));

    var multi = commandDispatcher.<Test1Command, TestEvent>execute(test1Command);
    AssertSubscriber<TestEvent> subscriber =
        multi.subscribe().withSubscriber(AssertSubscriber.create(2));

    var result = subscriber.awaitCompletion().assertCompleted().getItems();
    Assertions.assertThat(result)
        .hasSize(2)
        .extracting(TestEvent::getName)
        .containsExactlyInAnyOrder("q1", "q1b");
  }

  @Test
  void secondTestQueryShouldBeHandledProperly() {
    CommandDispatcher commandDispatcher = provideInstanceOfDispatcher(true);

    var test2Command = Test2Command.builder().build();
    commandDispatcher.execute(test2Command);

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(1)).secondHandlerMethod(test2Command);
    verify(testHandler, times(0)).thirdHandlerMethod(any());
  }

  private CommandDispatcher provideInstanceOfDispatcher(boolean resolveBeans) {
    var commandDispatcherRecorder = new CommandDispatcherRecorder();
    var commandDispatcherSupplier =
        commandDispatcherRecorder.createCommandDispatcherSupplier(
            olisteConfig, List.of(TestCommandHandlers.class.getName()));

    var commandDispatcher = commandDispatcherSupplier.get();
    Assertions.assertThat(commandDispatcher).isNotNull().isInstanceOf(CommandDispatcher.class);
    if (resolveBeans) {
      commandDispatcher.resolveBeans();
    }
    return commandDispatcher;
  }

  @Getter
  @SuperBuilder
  public static class Test1Command implements Command {}

  @Getter
  @SuperBuilder
  public static class Test2Command implements Command {}

  @Getter
  @SuperBuilder
  public static class UnhandledCommand implements Command {}

  @Jacksonized
  @Getter
  @SuperBuilder
  @EventDescriptor(eventName = "TestEvent", entityName = "TestEntity")
  public static class TestEvent extends Event {
    private final String name;
  }

  @ApplicationScoped
  private static class TestCommandHandlers {

    @CommandHandler
    public Multi<TestEvent> firstHandlerMethod(Test1Command query) {
      return Multi.createFrom().item(TestEvent.builder().name("F1").build());
    }

    @CommandHandler
    public Multi<TestEvent> thirdHandlerMethod(Test1Command query) {
      return Multi.createFrom().item(TestEvent.builder().name("F1b").build());
    }

    @CommandHandler
    public Multi<TestEvent> secondHandlerMethod(Test2Command query) {
      return Multi.createFrom().item(TestEvent.builder().name("F2").build());
    }
  }
}
