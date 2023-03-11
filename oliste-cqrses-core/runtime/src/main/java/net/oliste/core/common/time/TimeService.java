package net.oliste.core.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.enterprise.inject.spi.CDI;

public abstract class TimeService {
  private static TimeService instance = null;

  private static final TimeProvider timeProvider = new TimeProvider();

  public LocalDateTime getNow() {
    return timeProvider.now();
  }

  public LocalDate getToday() {
    return getNow().toLocalDate();
  }

  public TimeProvider provider() {
    return timeProvider;
  }

  public static class TimeProvider {
    private LocalDateTime stopTime = null;

    public LocalDateTime now() {
      return stopTime != null ? stopTime : LocalDateTime.now();
    }

    public void freezeTime() {
      stopTime = LocalDateTime.now();
    }

    public void resumeTime() {
      stopTime = null;
    }
  }

  public static TimeService getInstance() {
    if (instance == null) {
      instance = CDI.current().select(TimeService.class).get();
    }
    return instance;
  }
}
