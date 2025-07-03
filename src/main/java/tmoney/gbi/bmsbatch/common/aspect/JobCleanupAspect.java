// src/main/java/tmoney/tnz/bmsbatch/config/aop/JobCleanupAspect.java
package tmoney.gbi.bmsbatch.common.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.springframework.batch.core.JobExecution;
import tmoney.gbi.bmsbatch.common.service.BatchMetaTableCleanupService;


@Slf4j
//@Aspect
//@Component
@RequiredArgsConstructor
public class JobCleanupAspect {

    private final BatchMetaTableCleanupService cleanupService;

    // JobLauncher.run 메소드가 성공적으로 JobExecution 객체를 반환한 후에 이 로직을 실행합니다.
    @AfterReturning(
            pointcut = "execution(* org.springframework.batch.core.launch.JobLauncher.run(..))",
            returning = "jobExecution"
    )
    public void afterJobRuns(JobExecution jobExecution) {
        log.info("AOP detected job completion for: {}", jobExecution.getJobInstance().getJobName());
        cleanupService.cleanup(jobExecution);
    }
}