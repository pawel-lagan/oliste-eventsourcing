package net.oliste.jpa.es.persistance;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@IdClass(PersistentEventVersion.PersistentEventVersionPk.class)
public class PersistentEventVersion {

  @Id
  @Column(nullable = false, updatable = false)
  @Getter
  @Setter
  private Long entityId;

  @Id
  @Column(nullable = false, updatable = false)
  @Getter
  @Setter
  private String entityName;

  @Version @Getter @Setter private Long version;

  @Getter @Setter private LocalDateTime timestamp;

  PersistentEventVersion() {}

  static PersistentEventVersion newInstance() {
    PersistentEventVersion persistentEvent = new PersistentEventVersion();
    return persistentEvent;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  @ToString
  public static class PersistentEventVersionPk implements Serializable {
    private Long entityId;
    private String entityName;
  }
}
