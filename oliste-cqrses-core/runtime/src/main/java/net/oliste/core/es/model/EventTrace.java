package net.oliste.core.es.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public final class EventTrace {
  private final String traceId;
  private final String spanId;
  private final String parentId;
}
