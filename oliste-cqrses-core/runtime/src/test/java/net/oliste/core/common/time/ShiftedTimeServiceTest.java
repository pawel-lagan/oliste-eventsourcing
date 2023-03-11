package net.oliste.core.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

class ShiftedTimeServiceTest {
  private LocalDateTime currentTime = LocalDateTime.parse("2020-01-31T10:00:00");

  @Spy private ShiftedTimeService service = new ShiftedTimeService();

  @Test
  public void constructorTests() {
    final ShiftedTimeService noArgs = new ShiftedTimeService();
    final ShiftedTimeService ofLocalDateTime = new ShiftedTimeService(currentTime);

    assertThat(noArgs).isInstanceOf(ShiftedTimeService.class).isNotNull();
    assertThat(ofLocalDateTime).isInstanceOf(ShiftedTimeService.class).isNotNull();
  }

  @Test
  public void shouldReturnTimeWithAddedShift() {
    final ShiftedTimeService shift = new ShiftedTimeService(currentTime.plusHours(2));

    final LocalDateTime shiftNow = shift.getNow();

    assertThat(shiftNow).isAfter(currentTime);
  }

  @Test
  public void shouldShiftTimeTo() {
    final LocalDateTime tomorrow = currentTime.plusDays(1);

    service.shiftTimeTo(tomorrow);

    final long daysBetween = ChronoUnit.DAYS.between(service.getNow(), tomorrow);
    assertThat(daysBetween).isEqualTo(0);
  }

  @Test
  public void shouldShiftTimeWithLongPeriodBetweenDates() {
    final LocalDateTime futureDate = currentTime.plusYears(3).plusMonths(5).plusDays(15);

    service.shiftTimeTo(futureDate);

    final long daysBetween = ChronoUnit.DAYS.between(service.getNow(), futureDate);
    assertThat(daysBetween).isEqualTo(0);
  }

  @Test
  public void shouldShiftDateBackwards() {
    final LocalDateTime pastDate = currentTime.minusYears(1).minusMonths(2).minusDays(3);

    service.shiftTimeTo(pastDate);

    final long daysBetween = ChronoUnit.DAYS.between(service.getNow(), pastDate);
    assertThat(daysBetween).isEqualTo(0);
  }

  @Test
  public void shouldShiftDateIfStartDateIs30OfTheMonth() {
    currentTime = currentTime.withDayOfMonth(30);

    final LocalDateTime threeDaysAhead = currentTime.plusDays(3);

    service.shiftTimeTo(threeDaysAhead);

    final long daysBetween = ChronoUnit.DAYS.between(service.getNow(), threeDaysAhead);
    assertThat(daysBetween).isEqualTo(0);
  }

  @Test
  public void shouldResetShift() {
    service.shiftTimeTo(currentTime.plusDays(1));

    service.resetShift();

    assertThat(service.getToday()).isEqualTo(LocalDate.now());
  }
}
