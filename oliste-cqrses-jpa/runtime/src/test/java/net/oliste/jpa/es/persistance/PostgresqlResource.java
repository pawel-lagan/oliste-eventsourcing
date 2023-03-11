package net.oliste.jpa.es.persistance;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresqlResource implements QuarkusTestResourceLifecycleManager {

  static PostgreSQLContainer<?> db =
      new PostgreSQLContainer<>("postgres:latest")
          .withDatabaseName("integration_test_db")
          .withUsername("user")
          .withPassword("password");

  @Override
  public Map<String, String> start() {
    db.start();
    var url = db.getJdbcUrl().replaceFirst("jdbc\\:", "");
    System.out.println(url);
    return Map.of(
        "quarkus.datasource.reactive.url", url, "quarkus.datasource.jdbc.url", "jdbc:" + url);
  }

  @Override
  public void stop() {
    db.stop();
  }
}
