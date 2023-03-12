package net.oliste.reactive.liquibase;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.ExceptionUtil;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LiquibaseMigration {
  private static final Logger LOG = Logger.getLogger(LiquibaseMigration.class);

  @ConfigProperty(name = "oliste.liquibase.migrate")
  boolean runMigration;

  @ConfigProperty(name = "quarkus.datasource.reactive.url")
  String datasourceUrl;

  @ConfigProperty(name = "quarkus.datasource.username")
  String datasourceUsername;

  @ConfigProperty(name = "quarkus.datasource.password")
  String datasourcePassword;

  @ConfigProperty(name = "quarkus.liquibase.change-log")
  String changeLogLocation;

  public void runLiquibaseMigration(@Observes StartupEvent event) throws LiquibaseException {
    if (runMigration) {
      LOG.info("Liquibase Migration Start");
      Liquibase liquibase = null;
      try {
        ResourceAccessor resourceAccessor =
            new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
        DatabaseConnection conn =
            DatabaseFactory.getInstance()
                .openConnection(
                    "jdbc:" + datasourceUrl,
                    datasourceUsername,
                    datasourcePassword,
                    null,
                    resourceAccessor);
        liquibase = new Liquibase(changeLogLocation, resourceAccessor, conn);
        liquibase.update(new Contexts(), new LabelExpression());
      } catch (Exception e) {
        LOG.error("Liquibase Migration Exception: " + ExceptionUtil.generateStackTrace(e));
        throw e;
      } finally {
        if (liquibase != null) {
          liquibase.close();
          LOG.info("Liquibase Migration End");
        }
      }
    } else {
      LOG.info("Liquibase Migration Disabled");
    }
  }
}
