package net.oliste.examples.process;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import javax.enterprise.event.Observes;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.oliste.core.cqrs.command.CommandDispatcher;
import net.oliste.core.cqrs.query.QueryDispatcher;
import net.oliste.dynamodb.es.persistance.DynamoDbPersistentEventRepository;

@RequiredArgsConstructor
public class ProcessResource {
  private final CommandDispatcher commandDispatcher;
  private final QueryDispatcher queryDispatcher;
  private final ProcessProjector processProjector;
  private final ProcessEventStore eventStore;
  private final DynamoDbPersistentEventRepository dynamoDbPersistentEventRepository;

  private long startTime = System.currentTimeMillis();

  @Value
  @RegisterForReflection
  static class ProcessData {
    private Long id;
    private String name;
  }

  void onStart(@Observes StartupEvent startup) {
    dynamoDbPersistentEventRepository.createTableIfNotExists().subscribe().with((s) -> {});
    reportTime("Init time ", startTime);
  }

  @Route(path = "/processes", methods = Route.HttpMethod.POST)
  Uni<Response> createTask(@Body ProcessData processData, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    ProcessCommandHandlers.CreateTaskCommand createTaskCommand =
        ProcessCommandHandlers.CreateTaskCommand.builder().name(processData.getName()).build();

    return commandDispatcher
        .<ProcessCommandHandlers.CreateTaskCommand, ProcessEvent>execute(createTaskCommand)
        .onItem()
        .transformToUniAndConcatenate(event -> eventStore.publishEvent(event))
        .collect()
        .asList()
        .onItem()
        .transform(
            val -> {
              reportTime("Response time ", currentTime);
              return Response.ok().status(CREATED).build();
            });
  }

  private long reportTime(String s, long baseTime) {
    long currentTime = System.currentTimeMillis();
    long startupTime = currentTime - baseTime;
    System.out.println(s + startupTime + " ms");
    return currentTime;
  }

  @Route(path = "/processes/:id", methods = Route.HttpMethod.PUT)
  Uni<Response> updateTask(
      @Body ProcessData processData, @Param("id") Long id, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    ProcessCommandHandlers.UpdateTaskCommand updateTaskCommand =
        ProcessCommandHandlers.UpdateTaskCommand.builder()
            .id(id)
            .name(processData.getName())
            .build();

    return commandDispatcher
        .<ProcessCommandHandlers.UpdateTaskCommand, ProcessEvent>execute(updateTaskCommand)
        .onItem()
        .transformToUniAndConcatenate(event -> eventStore.publishEvent(event))
        .collect()
        .asList()
        .onItem()
        .transform(
            val -> {
              reportTime("Response time ", currentTime);
              return Response.ok().status(OK).build();
            });
  }

  @Route(path = "/processes", methods = Route.HttpMethod.GET)
  Uni<Response> all(RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);
    ProcessQueryHandlers.GetAllProcessesQuery allTasksQuery =
        ProcessQueryHandlers.GetAllProcessesQuery.builder().build();
    return queryDispatcher
        .query(allTasksQuery)
        .onItem()
        .transform(
            list -> {
              reportTime("Response time ", currentTime);
              return Response.ok(list).status(CREATED).build();
            });
  }

  @Route(path = "/processes/:id", methods = Route.HttpMethod.GET)
  Uni<Response> all(@Param("id") Long id, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    ProcessQueryHandlers.GetProcessByIdQuery getProcessByIdQuery =
        ProcessQueryHandlers.GetProcessByIdQuery.builder().id(id).build();
    return queryDispatcher
        .query(getProcessByIdQuery)
        .onItem()
        .transform(
            task -> {
              reportTime("Response time ", currentTime);
              return Response.ok(task).status(CREATED).build();
            });
  }

  @Route(path = "/processes-cache", methods = Route.HttpMethod.GET)
  Uni<Response> cache(RoutingContext context) {
    return Uni.createFrom()
        .item(processProjector.getProjectionCache())
        .onItem()
        .transform(
            list -> {
              return Response.ok(list).status(CREATED).build();
            });
  }
}
