package net.oliste.examples.process;

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
public class ProcessQueryHandlers {
  private final ProcessRepository processRepository;
  private final ProcessEventRepository processEventRepository;

  @Getter
  @SuperBuilder
  public static class GetAllProcessesQuery implements Query {}

  @Getter
  @SuperBuilder
  public static class GetProcessByIdQuery implements Query {
    private final Long id;
  }

  @QueryHandler
  public Uni<List<Process>> getAllTasks(GetAllProcessesQuery query) {
    final List<Process> list = new ArrayList<>();

    return processEventRepository.loadAllEntities(Process.class);
  }

  @QueryHandler
  public Uni<Process> getById(GetProcessByIdQuery query) {
    return processEventRepository.loadEntity(query.getId(), Process.class);
  }
}
