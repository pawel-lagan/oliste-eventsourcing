package net.oliste.dynamodb.es.persistance;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.core.es.persistance.PersistentEventRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@Unremovable
public class DynamoDbPersistentEventRepository
    implements PersistentEventRepository<PersistentEvent> {

  private final DynamoDbEnhancedAsyncClient enhancedClient;
  private final Snowflake snowFlake;
  private final String tableName;

  public DynamoDbPersistentEventRepository(
      DynamoDbEnhancedAsyncClient enhancedClient, Snowflake snowFlake, String tableName) {
    this.enhancedClient = enhancedClient;
    this.snowFlake = snowFlake;
    this.tableName = tableName;
  }

  public String getTableName() {
    return this.tableName;
  }

  public String getVersionTableName() {
    return this.getTableName() + "_version";
  }

  public Uni<Long> getNextPersistentEventId() {
    return Uni.createFrom().item(snowFlake.nextId());
  }

  @Override
  public Uni<List<PersistentEvent>> getAll() {
    var persistentEventTable = getPersistentEventTable();

    return Uni.createFrom()
        .publisher(persistentEventTable.scan())
        .onItem()
        .transform(res -> res.items().stream().collect(Collectors.toList()));
  }

  @Override
  public Uni<List<PersistentEvent>> getAllByEntity(String entityName) {
    var persistentEvent2ndIndex = getPersistentEventTable().index(PersistentEvent.SECONDARY_INDEX);
    var persistentEventTable = getPersistentEventTable();

    return Uni.createFrom()
        .publisher(
            persistentEvent2ndIndex.query(
                QueryConditional.sortGreaterThanOrEqualTo(
                    Key.builder().partitionValue(entityName).sortValue(entityName).build())))
        /*persistentEventTable.query(
        QueryConditional.sortBeginsWith(Key.builder()
                        .partitionValue(entityName)
                        .sortValue(entityName)
                        .build())))*/
        .onItem()
        .transform(res -> res.items().stream().collect(Collectors.toList()));
  }

  @Override
  public Uni<List<PersistentEvent>> getAllByEntityId(String entityName, Long entityId) {
    var persistentEventTable = getPersistentEventTable();

    return Uni.createFrom()
        .publisher(
            persistentEventTable.query(
                QueryConditional.keyEqualTo(
                    Key.builder()
                        .partitionValue(PersistentEvent.makeKey(entityName, entityId))
                        .build())))
        .onItem()
        .transform(res -> res.items().stream().collect(Collectors.toList()));
  }

  @Override
  public Uni<Long> getNextEntityId(String entityName) {
    return Uni.createFrom().item(snowFlake.nextId());
  }

  public Uni<Long> getNextEntityVersion(String entityName, long entityId) {
    var item = PersistentEventVersion.newInstance();
    item.setId(PersistentEventVersion.makeKey(entityName, entityId));
    item.setEntityName(entityName);
    item.setEntityId(entityId);

    var enhancedRequest =
        UpdateItemEnhancedRequest.<PersistentEventVersion>builder(PersistentEventVersion.class)
            .item(item)
            .ignoreNulls(Boolean.TRUE)
            .build();

    var persistentEventVersionTable = getPersistentEventVersionmTable();
    return Uni.createFrom()
        .completionStage(persistentEventVersionTable.updateItemWithResponse(enhancedRequest))
        .onItem()
        .transformToUni(res -> Uni.createFrom().item(res.attributes().getVersion()));
  }

  @Override
  public Uni<Long> getLastEntityVersion(String entityName, long entityId) {
    var persistentEventTable = getPersistentEventTable();

    return Uni.createFrom()
        .publisher(
            persistentEventTable.query(
                QueryConditional.sortGreaterThan(
                    Key.builder()
                        .partitionValue(PersistentEvent.makeKey(entityName, entityId))
                        .sortValue(0)
                        .build())))
        .onItem()
        .transform(
            res ->
                res.items().stream()
                    .map(PersistentEvent::getVersion)
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElse(0L));
  }

  @Override
  public Uni<PersistentEvent> saveWithVersion(PersistentEvent event) {
    var persistentEventTable = getPersistentEventTable();

    if (event.getEntityId() == null) {
      return getNextEntityId(event.getEntityName())
          .onItem()
          .transformToUni(
              entityId -> {
                return getNextEntityVersion(event.getEntityName(), entityId)
                    .onItem()
                    .transformToUni(
                        version -> {
                          var id = PersistentEvent.makeKey(event.getEntityName(), entityId);
                          event.setId(id);
                          event.setEntityId(entityId);
                          event.setVersion(version);
                          return createNewPersistentEvent(event, persistentEventTable, id);
                        });
              });
    }

    return getNextEntityVersion(event.getEntityName(), event.getEntityId())
        .onItem()
        .transformToUni(
            version -> {
              var id = PersistentEvent.makeKey(event.getEntityName(), event.getEntityId());
              event.setId(id);
              event.setVersion(version);
              return createNewPersistentEvent(event, persistentEventTable, id);
            });
  }

  private static Uni<PersistentEvent> createNewPersistentEvent(
      PersistentEvent event, DynamoDbAsyncTable<PersistentEvent> persistentEventTable, String id) {
    event.setId(id);
    return Uni.createFrom()
        .completionStage(persistentEventTable.putItem(event))
        .onItem()
        .transformToUni(item -> Uni.createFrom().item(event));
  }

  @Override
  public PersistentEvent createNewPersistentEvent() {
    return PersistentEvent.newInstance();
  }

  private DynamoDbAsyncTable<PersistentEvent> getPersistentEventTable() {
    return enhancedClient.table(getTableName(), TableSchema.fromBean(PersistentEvent.class));
  }

  private DynamoDbAsyncTable<PersistentEventVersion> getPersistentEventVersionmTable() {
    return enhancedClient.table(
        getVersionTableName(), TableSchema.fromBean(PersistentEventVersion.class));
  }

  public Uni<Void> createTableIfNotExists() {
    var persistentEventTable = getPersistentEventTable();
    var persistentEventVersionTable = getPersistentEventVersionmTable();
    var persistentEvent2ndIndex = persistentEventTable.index(PersistentEvent.SECONDARY_INDEX);

    var persistentEventUni =
        Uni.createFrom()
            .completionStage(persistentEventTable.describeTable())
            .onFailure()
            .recoverWithUni(
                exc -> {
                  return Uni.createFrom()
                      .completionStage(
                          persistentEventTable.createTable(
                              CreateTableEnhancedRequest.builder()
                                  .globalSecondaryIndices(
                                      EnhancedGlobalSecondaryIndex.builder()
                                          .indexName(persistentEvent2ndIndex.indexName())
                                          .projection(
                                              Projection.builder()
                                                  .projectionType(ProjectionType.ALL)
                                                  .build())
                                          .build())
                                  .build()))
                      .onItem()
                      .transformToUni(
                          item ->
                              Uni.createFrom()
                                  .completionStage(persistentEventTable.describeTable()));
                });

    var persistentEventVersionUni =
        Uni.createFrom()
            .completionStage(persistentEventVersionTable.describeTable())
            .onFailure()
            .recoverWithUni(
                exc -> {
                  return Uni.createFrom()
                      .completionStage(persistentEventVersionTable.createTable())
                      .onItem()
                      .transformToUni(
                          item ->
                              Uni.createFrom()
                                  .completionStage(persistentEventVersionTable.describeTable()));
                });

    return Uni.combine()
        .all()
        .unis(persistentEventUni, persistentEventVersionUni)
        .asTuple()
        .onItem()
        .transformToUni(tuple -> Uni.createFrom().voidItem());
  }
}
