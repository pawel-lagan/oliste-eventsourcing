package net.oliste.reactive;

import io.quarkus.arc.DefaultBean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.jpa.es.persistance.JpaPersistentEventRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny;

@Dependent
public class AppConfig {
  @Inject
  @ConfigProperty(name = "oliste.es.dynamodb.table-name", defaultValue = "persistent_event")
  public String tableName = "persistent_event";

  @Produces
  @ApplicationScoped
  @DefaultBean
  public JpaPersistentEventRepository initializeEventStoreRepository(
      Mutiny.SessionFactory sessionFactory, Snowflake snowflake) {
    return new JpaPersistentEventRepository(sessionFactory, snowflake);
  }
}
