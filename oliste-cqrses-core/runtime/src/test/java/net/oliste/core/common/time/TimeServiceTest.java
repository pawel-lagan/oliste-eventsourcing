package net.oliste.core.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

class TimeServiceTest {
  @Spy private RealTimeService service = new RealTimeService();

  @Test
  public void shouldReturnTimeProvider() {
    TimeService.TimeProvider provider = service.provider();
    assertThat(provider).isNotNull().isExactlyInstanceOf(TimeService.TimeProvider.class);
  }

  @Test
  public void shouldReturnCurrentTime() throws InterruptedException {
    LocalDateTime now = service.getNow();
    Thread.sleep(20L);
    assertThat(now).isBefore(service.getNow());
  }

  @Test
  public void shouldFreezCurrentTime() throws InterruptedException {
    TimeService.TimeProvider provider = service.provider();
    provider.freezeTime();

    LocalDateTime now = service.getNow();

    Thread.sleep(20L);
    assertThat(now).isEqualTo(service.getNow());
    provider.resumeTime();
  }

  @Test
  public void shouldResumeCurrentTime() throws InterruptedException {
    TimeService.TimeProvider provider = service.provider();
    provider.freezeTime();

    LocalDateTime now = service.getNow();

    Thread.sleep(20L);
    provider.resumeTime();
    assertThat(now).isBefore(service.getNow());
  }
}
