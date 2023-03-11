package net.oliste.core.es.persistance;

public interface PersistentEvent<KeyT> {
  KeyT getId();

  Long getEntityId();

  String getEntityName();

  String getEventName();

  String getTraceId();

  String getSpanId();

  String getParentId();

  java.time.LocalDateTime getCreatedAt();

  String getEventData();

  Long getVersion();

  void setEntityId(Long entityId);

  void setEntityName(String entityName);

  void setEventName(String eventName);

  void setTraceId(String traceId);

  void setSpanId(String spanId);

  void setParentId(String parentId);

  void setCreatedAt(java.time.LocalDateTime createdAt);

  void setEventData(String eventData);

  void setVersion(Long version);
}
