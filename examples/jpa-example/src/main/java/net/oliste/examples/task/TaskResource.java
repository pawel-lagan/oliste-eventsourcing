package net.oliste.reactive.task;

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

@RequiredArgsConstructor
public class TaskResource {
  private final CommandDispatcher commandDispatcher;
  private final QueryDispatcher queryDispatcher;
  private final TaskProjector taskProjector;
  private final TaskEventStore eventStore;

  private long startTime = System.currentTimeMillis();

  @Value
  @RegisterForReflection
  static class TaskData {
    private Long id;
    private String name;
  }

  void onStart(@Observes StartupEvent startup) {
    reportTime("Init time ", startTime);
  }

  @Route(path = "/tasks", methods = Route.HttpMethod.POST)
  Uni<Response> createTask(@Body TaskData taskData, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    TaskCommandHandlers.CreateTaskCommand createTaskCommand =
        TaskCommandHandlers.CreateTaskCommand.builder().name(taskData.getName()).build();

    return commandDispatcher
        .<TaskCommandHandlers.CreateTaskCommand, TaskEvent>execute(createTaskCommand)
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

  @Route(path = "/tasks/:id", methods = Route.HttpMethod.PUT)
  Uni<Response> updateTask(@Body TaskData taskData, @Param("id") Long id, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    TaskCommandHandlers.UpdateTaskCommand updateTaskCommand =
        TaskCommandHandlers.UpdateTaskCommand.builder().id(id).name(taskData.getName()).build();

    return commandDispatcher
        .<TaskCommandHandlers.UpdateTaskCommand, TaskEvent>execute(updateTaskCommand)
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

  @Route(path = "/tasks", methods = Route.HttpMethod.GET)
  Uni<Response> all(RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);
    TaskQueryHandlers.GetAllTasksQuery allTasksQuery =
        TaskQueryHandlers.GetAllTasksQuery.builder().build();
    return queryDispatcher
        .query(allTasksQuery)
        .onItem()
        .transform(
            list -> {
              reportTime("Response time ", currentTime);
              return Response.ok(list).status(CREATED).build();
            });
  }

  @Route(path = "/tasks/:id", methods = Route.HttpMethod.GET)
  Uni<Response> all(@Param("id") Long id, RoutingContext context) {
    long currentTime = reportTime("Time since startup", startTime);

    TaskQueryHandlers.GetTaskByIdQuery getTaskByIdQuery =
        TaskQueryHandlers.GetTaskByIdQuery.builder().id(id).build();
    return queryDispatcher
        .query(getTaskByIdQuery)
        .onItem()
        .transform(
            task -> {
              reportTime("Response time ", currentTime);
              return Response.ok(task).status(CREATED).build();
            });
  }

  @Route(path = "/tasks-cache", methods = Route.HttpMethod.GET)
  Uni<Response> cache(RoutingContext context) {
    return Uni.createFrom()
        .item(taskProjector.getProjectionCache())
        .onItem()
        .transform(
            list -> {
              return Response.ok(list).status(CREATED).build();
            });
  }
}
