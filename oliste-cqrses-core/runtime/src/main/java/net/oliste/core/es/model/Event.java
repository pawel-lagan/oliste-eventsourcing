package net.oliste.core.es.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Event {
  @Setter private Long entityId;
  private final EventTrace trace;
  private final LocalDateTime createdAt;
}
