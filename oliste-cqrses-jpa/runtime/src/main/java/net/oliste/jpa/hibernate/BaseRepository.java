package net.oliste.jpa.hibernate;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

public abstract class BaseRepository<T extends BaseEntity> implements PanacheRepository<T> {
  @Inject @Any protected Mutiny.SessionFactory sessionFactory;

  public Mutiny.SessionFactory getSessionFactory() {
    return sessionFactory;
  }
}
