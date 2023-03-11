package net.oliste.core.common.time;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// @UnlessBuildProperty(name = Environment.ENVIRONMENT_PROPERTY, stringValue = Environment.PROD)
// @ApplicationScoped
// @Unremovable
public class ShiftedTimeService extends TimeService {
  private LocalDateTime baseTime;
  private long shift;

  public ShiftedTimeService() {
    this(LocalDateTime.now());
  }

  public ShiftedTimeService(LocalDateTime baseTime) {
    shiftTimeTo(baseTime);
  }

  @Override
  public LocalDateTime getNow() {
    return ChronoUnit.NANOS.addTo(super.getNow(), shift);
  }

  public void shiftTimeTo(LocalDateTime baseTime) {
    shift = ChronoUnit.NANOS.between(LocalDateTime.now(), baseTime);
    this.baseTime = baseTime;
  }

  public void resetShift() {
    shift = 0;
  }
}
