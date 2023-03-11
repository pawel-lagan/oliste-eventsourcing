package net.oliste.core.cqrs.query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.oliste.core.OlisteConfig;
import net.oliste.core.cqrs.QueriesDispatcherRecorder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class QueryDispatcherTest {

  @Mock private OlisteConfig olisteConfig;

  private TestQueryHandlers testHandler;

  @BeforeEach
  void setUp() {
    testHandler = mock(TestQueryHandlers.class);
    QuarkusMock.installMockForType(testHandler, TestQueryHandlers.class);
  }

  @Test
  void exceptionShouldBeThrownWhenThereIsNoQueryHandlerRegistered() {
    QueryDispatcher queryDispatcher = provideInstanceOfDispatcher(true);

    var testQuery = UnhandledQuery.builder().build();

    Assertions.assertThatThrownBy(() -> queryDispatcher.query(testQuery))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No query handler for query ");

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(0)).secondHandlerMethod(any());
  }

  @Test
  void exceptionShouldBeThrownWhenThereIsNoBeanForObjectHandler() {
    QueryDispatcher queryDispatcher = provideInstanceOfDispatcher(false);

    var testQuery = Test1Query.builder().build();

    Assertions.assertThatThrownBy(() -> queryDispatcher.query(testQuery))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Handling object not registered for ");

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(0)).secondHandlerMethod(any());
  }

  @Test
  void firstTestQueryShouldBeHandledProperly() {
    QueryDispatcher queryDispatcher = provideInstanceOfDispatcher(true);

    var testQuery = Test1Query.builder().build();
    queryDispatcher.query(testQuery);

    verify(testHandler, times(1)).firstHandlerMethod(testQuery);
    verify(testHandler, times(0)).secondHandlerMethod(any());
  }

  @Test
  void secondTestQueryShouldBeHandledProperly() {
    QueryDispatcher queryDispatcher = provideInstanceOfDispatcher(true);

    var testQuery = Test2Query.builder().build();
    queryDispatcher.query(testQuery);

    verify(testHandler, times(0)).firstHandlerMethod(any());
    verify(testHandler, times(1)).secondHandlerMethod(testQuery);
  }

  private QueryDispatcher provideInstanceOfDispatcher(boolean resolveBeans) {
    var queriesDispatcherRecorder = new QueriesDispatcherRecorder();
    var queryDispatcherSupplier =
        queriesDispatcherRecorder.queriesDispatcherSupplier(
            olisteConfig, List.of(TestQueryHandlers.class.getName()));

    var queryDispatcher = queryDispatcherSupplier.get();
    Assertions.assertThat(queryDispatcher).isNotNull().isInstanceOf(QueryDispatcher.class);
    if (resolveBeans) {
      queryDispatcher.resolveBeans();
    }
    return queryDispatcher;
  }

  @Getter
  @SuperBuilder
  public static class Test1Query implements Query {}

  @Getter
  @SuperBuilder
  public static class Test2Query implements Query {}

  @Getter
  @SuperBuilder
  public static class UnhandledQuery implements Query {}

  @ApplicationScoped
  private static class TestQueryHandlers {

    @QueryHandler
    public void firstHandlerMethod(Test1Query query) {}

    @QueryHandler
    public void secondHandlerMethod(Test2Query query) {}
  }
}
