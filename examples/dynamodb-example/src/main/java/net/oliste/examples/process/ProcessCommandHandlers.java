package net.oliste.examples.process;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Multi;
import javax.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.oliste.core.cqrs.command.Command;
import net.oliste.core.cqrs.command.CommandHandler;

@ApplicationScoped
@Unremovable
@RequiredArgsConstructor
public class ProcessCommandHandlers {
  private final ProcessEventStore eventStore;

  @Getter
  @SuperBuilder
  public static class CreateTaskCommand implements Command {
    private final String name;
  }

  @Getter
  @SuperBuilder
  public static class UpdateTaskCommand implements Command {
    private final long id;
    private final String name;
  }

  @CommandHandler
  public Multi<ProcessEvent> handleUpdateTaskCommand(UpdateTaskCommand command) {
    Process.ProcessUpdateEvent updateEvent =
        Process.ProcessUpdateEvent.builder()
            .entityId(command.getId())
            .name(command.getName())
            .build();
    return Multi.createFrom().item(updateEvent);
  }

  @CommandHandler
  public Multi<ProcessEvent> handleCreateTaskCommand(CreateTaskCommand command) {
    Process.ProcessUpdateEvent updateEvent =
        Process.ProcessUpdateEvent.builder().name(command.getName()).build();
    return Multi.createFrom().item(updateEvent);
  }
}
