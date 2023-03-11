package net.oliste.dynamodb.es.persistance;

import io.quarkus.arc.Unremovable;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Unremovable
@DynamoDbBean
public class PersistentEventVersion {

  private String id;

  @Setter private Long entityId;

  private String entityName;

  private Long version;

  public PersistentEventVersion() {}

  static PersistentEventVersion newInstance() {
    PersistentEventVersion persistentEvent = new PersistentEventVersion();
    return persistentEvent;
  }

  public static String makeKey(String entityName, Long entityId) {
    return entityName + "@" + String.valueOf(entityId);
  }

  @DynamoDbAttribute("version")
  @DynamoDbAtomicCounter
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @DynamoDbAttribute("entity_name")
  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  @DynamoDbAttribute("entity_id")
  public Long getEntityId() {
    return this.entityId;
  }
}
