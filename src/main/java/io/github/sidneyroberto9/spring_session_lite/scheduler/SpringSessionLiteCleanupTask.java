package io.github.sidneyroberto9.spring_session_lite.scheduler;

import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class SpringSessionLiteCleanupTask {

    private final SpringSessionLiteService sessionService;

    @Scheduled(cron = "${spring-session-lite.cleanup-cron:0 */30 * * * *}")
    public void deleteExpired() {
        sessionService.deleteExpired();
        log.info("Expired sessions cleanup executed");
    }
}
