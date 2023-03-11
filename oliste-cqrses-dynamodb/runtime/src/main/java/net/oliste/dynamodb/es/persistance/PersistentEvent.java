package net.oliste.dynamodb.es.persistance;

import io.quarkus.arc.Unremovable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import net.oliste.core.common.time.TimeService;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Unremovable
@DynamoDbBean
public class PersistentEvent implements net.oliste.core.es.persistance.PersistentEvent<String> {
  public static final String SECONDARY_INDEX = "entity-name-index";
  private String id;

  @Setter @Getter private Long entityId;

  private String entityName;

  private String eventName;

  private String traceId;

  private String spanId;

  private String parentId;

  private LocalDateTime createdAt;

  private String eventData;

  private Long version;

  public PersistentEvent() {}

  static PersistentEvent newInstance() {
    PersistentEvent persistentEvent = new PersistentEvent();
    persistentEvent.createdAt = TimeService.getInstance().getNow();
    return persistentEvent;
  }

  public static String makeKey(String entityName, Long entityId) {
    return entityName + "@" + String.valueOf(entityId);
  }

  @DynamoDbSortKey
  @DynamoDbAttribute("version")
  @Override
  public Long getVersion() {
    return version;
  }

  @Override
  public void setVersion(Long version) {
    this.version = version;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("id")
  @DynamoDbSecondarySortKey(indexNames = {SECONDARY_INDEX})
  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @DynamoDbAttribute("entity_name")
  @DynamoDbSecondaryPartitionKey(indexNames = {SECONDARY_INDEX})
  @Override
  public String getEntityName() {
    return entityName;
  }

  @Override
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  @DynamoDbAttribute("created_at")
  @Override
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @DynamoDbAttribute("event_data")
  @Override
  public String getEventData() {
    return eventData;
  }

  @Override
  public void setEventData(String eventData) {
    this.eventData = eventData;
  }

  @DynamoDbAttribute("event_name")
  @Override
  public String getEventName() {
    return eventName;
  }

  @Override
  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  @DynamoDbAttribute("trace_id")
  @Override
  public String getTraceId() {
    return traceId;
  }

  @Override
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  @DynamoDbAttribute("span_id")
  @Override
  public String getSpanId() {
    return spanId;
  }

  @Override
  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  @DynamoDbAttribute("parent_id")
  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }
}
