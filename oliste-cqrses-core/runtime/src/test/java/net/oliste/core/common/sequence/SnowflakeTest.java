package net.oliste.core.common.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class SnowflakeTest {

  @Test
  public void nextIdShouldGenerateIdWithCorrectBitsFilled() {
    Snowflake snowflake = new Snowflake(784);

    long beforeTimestamp = Instant.now().toEpochMilli();

    long id = snowflake.nextId();

    // Validate different parts of the Id
    long[] attrs = snowflake.parse(id);
    assertTrue(attrs[0] >= beforeTimestamp);
    assertEquals(784, attrs[1]);
    assertEquals(0, attrs[2]);
  }

  @Test
  public void nextIdShouldGenerateUniqueId() {
    Snowflake snowflake = new Snowflake(234);
    int iterations = 5000;

    // Validate that the IDs are not same even if they are generated in the same ms
    long[] ids = new long[iterations];
    for (int i = 0; i < iterations; i++) {
      ids[i] = snowflake.nextId();
    }

    for (int i = 0; i < ids.length; i++) {
      for (int j = i + 1; j < ids.length; j++) {
        assertNotEquals(ids[i], ids[j]);
      }
    }
  }

  @Test
  public void nextIdShouldGenerateUniqueIdIfCalledFromMultipleThreads()
      throws InterruptedException, ExecutionException {
    int numThreads = 50;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    Snowflake snowflake = new Snowflake(234);
    int iterations = 10000;

    // Validate that the IDs are not same even if they are generated in the same ms in different
    // threads
    List<Future<Long>> futures = new ArrayList<>(iterations);
    for (int i = 0; i < iterations; i++) {
      futures.add(
          executorService.submit(
              () -> {
                long id = snowflake.nextId();
                latch.countDown();
                return id;
              }));
    }

    latch.await();
    for (int i = 0; i < futures.size(); i++) {
      for (int j = i + 1; j < futures.size(); j++) {
        assertNotSame(futures.get(i).get(), futures.get(j).get());
      }
    }
  }
}
