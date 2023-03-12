package net.oliste.reactive.task;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.oliste.core.cqrs.query.Query;
import net.oliste.core.cqrs.query.QueryHandler;

@ApplicationScoped
@AllArgsConstructor
@Unremovable
public class TaskQueryHandlers {
  private final TaskRepository taskRepository;
  private final TaskEventRepository taskEventRepository;

  @Getter
  @SuperBuilder
  public static class GetAllTasksQuery implements Query {}

  @Getter
  @SuperBuilder
  public static class GetTaskByIdQuery implements Query {
    private final Long id;
  }

  @QueryHandler
  public Uni<List<net.oliste.reactive.task.Task>> getAllTasks(GetAllTasksQuery query) {
    final List<net.oliste.reactive.task.Task> list = new ArrayList<>();

    return taskEventRepository.loadAllEntities(net.oliste.reactive.task.Task.class);
  }

  @QueryHandler
  public Uni<net.oliste.reactive.task.Task> getById(GetTaskByIdQuery query) {
    return taskEventRepository.loadEntity(query.getId(), Task.class);
  }
}
