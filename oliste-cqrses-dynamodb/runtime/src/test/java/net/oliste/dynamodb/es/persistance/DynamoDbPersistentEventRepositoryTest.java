package net.oliste.dynamodb.es.persistance;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import net.oliste.core.common.DispatcherHelper;
import net.oliste.core.common.sequence.Snowflake;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@QuarkusTest
class DynamoDbPersistentEventRepositoryTest {
  public static final long ENTITY_ID = 100L;
  public static final String ENTITY_1 = "Entity1";

  DynamoDbPersistentEventRepository dynamoDbPersistentEventRepository;

  @Inject DynamoDbEnhancedAsyncClient enhancedClient;
  @Inject Snowflake snowflake;

  @Test
  void getNextPersistentEventId() {}

  @BeforeEach
  void prepare() {
    dynamoDbPersistentEventRepository =
        new DynamoDbPersistentEventRepository(enhancedClient, snowflake, "junittable");

    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getTableName(),
                TableSchema.fromBean(PersistentEvent.class))
            .deleteTable());
    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getVersionTableName(),
                TableSchema.fromBean(PersistentEventVersion.class))
            .deleteTable());

    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getTableName(),
                TableSchema.fromBean(PersistentEvent.class))
            .createTable(
                CreateTableEnhancedRequest.builder()
                    .globalSecondaryIndices(
                        EnhancedGlobalSecondaryIndex.builder()
                            .indexName(PersistentEvent.SECONDARY_INDEX)
                            .projection(
                                Projection.builder().projectionType(ProjectionType.ALL).build())
                            .build())
                    .build()));

    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getVersionTableName(),
                TableSchema.fromBean(PersistentEventVersion.class))
            .createTable());
  }

  private static <T> T awaitForCompletition(CompletableFuture<T> createTableCompletableFeature) {
    return Uni.createFrom()
        .completionStage(createTableCompletableFeature)
        .onFailure()
        .recoverWithNull()
        .await()
        .atMost(Duration.ofMillis(60000));
  }

  private static <T> T awaitForCompletition(Uni<T> createTableCompletableFeature) {
    return createTableCompletableFeature.await().atMost(Duration.ofMillis(30000));
  }

  @AfterEach
  void cleanup() {
    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getTableName(),
                TableSchema.fromBean(PersistentEvent.class))
            .deleteTable());
    awaitForCompletition(
        enhancedClient
            .table(
                dynamoDbPersistentEventRepository.getVersionTableName(),
                TableSchema.fromBean(PersistentEventVersion.class))
            .deleteTable());
  }

  @Test
  void eventWithEmptyEntityIdWillCreateNewEntity() {
    PersistentEvent event = createTestPersistanceEvent(null, ENTITY_1);

    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(saved -> dynamoDbPersistentEventRepository.getAll()));

    Assertions.assertThat(result).isNotEmpty().extracting(PersistentEvent::getEntityId).isNotNull();
    Assertions.assertThat(result)
        .isNotEmpty()
        .extracting(PersistentEvent::getVersion)
        .containsExactly(0L);
  }

  @Test
  void lastVersionShouldBeFound() {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);

    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(
                    saved ->
                        dynamoDbPersistentEventRepository.getLastEntityVersion(
                            event.getEntityName(), event.getEntityId())));

    Assertions.assertThat(result).isEqualTo(0);

    var result2 =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(
                    saved ->
                        dynamoDbPersistentEventRepository.getLastEntityVersion(
                            event.getEntityName(), event.getEntityId())));

    var all = awaitForCompletition(dynamoDbPersistentEventRepository.getAll());

    Assertions.assertThat(all).hasSize(2);
    Assertions.assertThat(result2).isEqualTo(1);
  }

  @Test
  void eventWithEntityIdWillCreateNewVersion() {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);
    PersistentEvent event2 = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);

    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(saved -> dynamoDbPersistentEventRepository.getAll()));

    Assertions.assertThat(result)
        .isNotEmpty()
        .extracting(PersistentEvent::getEntityId)
        .containsExactly(ENTITY_ID);

    Assertions.assertThat(result)
        .isNotEmpty()
        .extracting(PersistentEvent::getVersion)
        .containsExactly(0L);

    var result2 =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event2)
                .onItem()
                .transformToUni(saved -> dynamoDbPersistentEventRepository.getAll()));

    Assertions.assertThat(result2)
        .hasSize(2)
        .extracting(PersistentEvent::getEntityId)
        .containsExactly(ENTITY_ID, ENTITY_ID);
    Assertions.assertThat(result2)
        .isNotEmpty()
        .extracting(PersistentEvent::getVersion)
        .containsExactly(0L, 1L);
  }

  @Test
  void allEventsForSpecifiedEntityWillBeReturned() {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);

    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(
                    saved ->
                        dynamoDbPersistentEventRepository.getAllByEntityId(ENTITY_1, ENTITY_ID)));

    Assertions.assertThat(result)
        .hasSize(1)
        .extracting(PersistentEvent::getEntityId)
        .containsExactly(ENTITY_ID);
  }

  @Test
  void allEventsForSpecifiedEntityNameWillBeReturned() {
    PersistentEvent event = createTestPersistanceEvent(ENTITY_ID, ENTITY_1);
    PersistentEvent event2 = createTestPersistanceEvent(ENTITY_ID, "Entity2");

    awaitForCompletition(dynamoDbPersistentEventRepository.saveWithVersion(event2));

    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository
                .saveWithVersion(event)
                .onItem()
                .transformToUni(
                    saved -> dynamoDbPersistentEventRepository.getAllByEntity(ENTITY_1)));

    Assertions.assertThat(result)
        .hasSize(1)
        .extracting(PersistentEvent::getEntityId)
        .containsExactly(ENTITY_ID);
  }

  @Test
  void getNextVersion() {
    var result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID));

    Assertions.assertThat(result).isEqualTo(0);

    result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository.getNextEntityVersion(ENTITY_1 + "mod", ENTITY_ID));

    Assertions.assertThat(result).isEqualTo(0);

    result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID));

    Assertions.assertThat(result).isEqualTo(1);

    result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository.getNextEntityVersion(ENTITY_1, ENTITY_ID));

    Assertions.assertThat(result).isEqualTo(2);

    result =
        awaitForCompletition(
            dynamoDbPersistentEventRepository.getNextEntityVersion(ENTITY_1 + "mod", ENTITY_ID));

    Assertions.assertThat(result).isEqualTo(1);
  }

  @Test
  void createsTablesIfNotExists() {
    cleanup();
    awaitForCompletition(dynamoDbPersistentEventRepository.createTableIfNotExists());
  }

  @Test
  void doNothingWhenTablesExists() {
    awaitForCompletition(dynamoDbPersistentEventRepository.createTableIfNotExists());
  }

  private static PersistentEvent createTestPersistanceEvent(Long entityId, String entityName) {
    var event = PersistentEvent.newInstance();
    event.setEventData("{DD}");
    event.setEventName("Event name " + entityId + LocalDateTime.now());
    event.setEntityId(entityId);
    event.setEntityName(entityName);
    event.setCreatedAt(LocalDateTime.now());
    event.setParentId("AB");
    event.setSpanId("BC");
    event.setTraceId("TC");
    return event;
  }

  @Dependent
  public static class AppConfig {
    @ApplicationScoped
    DispatcherHelper dispatcherHelper() {
      return new DispatcherHelper();
    }
  }
}
