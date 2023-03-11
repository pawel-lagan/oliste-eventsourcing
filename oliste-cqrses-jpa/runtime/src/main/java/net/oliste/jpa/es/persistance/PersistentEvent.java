package net.oliste.jpa.es.persistance;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.oliste.core.common.time.TimeService;

@Entity
public class PersistentEvent implements net.oliste.core.es.persistance.PersistentEvent<Long> {
  @Id
  @Column(nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Getter
  private Long id;

  @Getter @Setter private Long entityId;

  @Getter @Setter private String entityName;

  @Getter
  @Setter
  @Size(max = 255)
  private String eventName;

  @Getter
  @Setter
  @Size(max = 32)
  private String traceId;

  @Getter
  @Setter
  @Size(max = 32)
  private String spanId;

  @Getter
  @Setter
  @Size(max = 32)
  private String parentId;

  @Getter @Setter private LocalDateTime createdAt;

  @Getter @Setter private String eventData;

  @Version @Getter @Setter private Long version;

  PersistentEvent() {}

  static PersistentEvent newInstance() {
    PersistentEvent persistentEvent = new PersistentEvent();
    persistentEvent.createdAt = TimeService.getInstance().getNow();
    return persistentEvent;
  }
}
