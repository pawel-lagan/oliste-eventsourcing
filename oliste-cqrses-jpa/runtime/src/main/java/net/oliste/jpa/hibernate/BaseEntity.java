package net.oliste.jpa.hibernate;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import net.oliste.core.common.time.TimeService;
import org.hibernate.Hibernate;

@MappedSuperclass
public class BaseEntity {
  @Id
  @Column(nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Getter
  @Setter
  private Long id;

  @Column(nullable = false, updatable = false, unique = true)
  @Getter
  private UUID uuid;

  @Version @Getter @Setter private Long version;

  @Getter @Setter private LocalDateTime createdAt;

  protected BaseEntity(boolean generateUuid) {
    if (generateUuid) {
      uuid = UUID.randomUUID();
      createdAt = TimeService.getInstance().getNow();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "#" + (getId() != null ? getId() : "???");
  }

  @Override
  public int hashCode() {
    return getUuid().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    final Object unproxy = Hibernate.unproxy(obj);
    if (unproxy instanceof BaseEntity) {
      return getUuid().equals(((BaseEntity) unproxy).getUuid());
    }
    return false;
  }
}
