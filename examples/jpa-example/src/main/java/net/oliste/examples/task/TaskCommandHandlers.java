package net.oliste.reactive.task;

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
public class TaskCommandHandlers {
  private final TaskEventStore eventStore;

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
  public Multi<TaskEvent> handleUpdateTaskCommand(UpdateTaskCommand command) {
    net.oliste.reactive.task.Task.TaskUpdateEvent updateEvent =
        net.oliste.reactive.task.Task.TaskUpdateEvent.builder()
            .entityId(command.getId())
            .name(command.getName())
            .build();
    return Multi.createFrom().item(updateEvent);
  }

  @CommandHandler
  public Multi<TaskEvent> handleCreateTaskCommand(CreateTaskCommand command) {
    net.oliste.reactive.task.Task.TaskUpdateEvent updateEvent =
        Task.TaskUpdateEvent.builder().name(command.getName()).build();
    return Multi.createFrom().item(updateEvent);
  }
}
