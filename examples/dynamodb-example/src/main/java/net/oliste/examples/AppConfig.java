package net.oliste.examples;

import io.quarkus.arc.DefaultBean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import net.oliste.core.common.sequence.Snowflake;
import net.oliste.dynamodb.es.persistance.DynamoDbPersistentEventRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Dependent
public class AppConfig {
  @Inject
  @ConfigProperty(name = "oliste.es.dynamodb.table-name", defaultValue = "persistent_event")
  public String tableName = "persistent_event";

  @Produces
  @ApplicationScoped
  @DefaultBean
  public DynamoDbPersistentEventRepository initializeEventStoreRepository2(
      DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, Snowflake snowflake) {
    return new DynamoDbPersistentEventRepository(dynamoDbEnhancedAsyncClient, snowflake, tableName);
  }
}
