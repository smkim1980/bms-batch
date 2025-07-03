package tmoney.gbi.bmsbatch.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.stereotype.Component;

/**
 * AOP를 사용하여 Spring Batch Job 실행 전후로 MDC(Mapped Diagnostic Context)에
 * Job 이름을 설정하고 제거합니다. 이를 통해 Logback의 SiftingAppender가
 * Job별로 로그 파일을 동적으로 생성할 수 있도록 지원합니다.
 */
@Aspect
@Component
@Slf4j
public class JobMdcAspect {


    private static final String JOB_NAME_MDC_KEY = "jobName";
    private static final String UNKNOWN_JOB = "UNKNOWN_JOB";

    /**
     * JobLauncher의 run 메소드 실행을 Pointcut으로 지정합니다.
     * 이 메소드는 모든 Job 실행의 시작점입니다.
     */
    @Pointcut("execution(* org.springframework.batch.core.launch.JobLauncher.run(..))")
    public void jobLauncherRun() {
    }

    /**
     * JobLauncher.run 메소드가 실행되기 전에 호출됩니다.
     * Job 객체에서 Job 이름을 추출하여 MDC에 설정합니다.
     *
     * @param joinPoint 프록시된 메소드에 대한 정보를 담고 있습니다.
     */
    @Before("jobLauncherRun()")
    public void beforeJob(JoinPoint joinPoint) {
        try {
            String jobName = extractJobName(joinPoint);
            MDC.put(JOB_NAME_MDC_KEY, jobName);
            log.debug("MDC 에 Job 이름 설정: {}", jobName);
        } catch (Exception e) {
            log.warn("Job 이름을 MDC 에 설정하는 중 오류 발생", e);
            MDC.put(JOB_NAME_MDC_KEY, UNKNOWN_JOB);
        }
    }

    /**
     * JobLauncher.run 메소드가 성공적으로 끝나거나 예외로 종료된 후 항상 호출됩니다.
     * MDC에서 Job 이름을 제거합니다.
     */
    @After("jobLauncherRun()")
    public void afterJob() {
        try {
            String removedJobName = MDC.get(JOB_NAME_MDC_KEY);
            MDC.remove(JOB_NAME_MDC_KEY);
            log.debug("MDC에서 Job 이름 제거: {}", removedJobName);
        } catch (Exception e) {
            log.warn("MDC 에서 Job 이름을 제거하는 중 오류 발생", e);
        }
    }

    /**
     * JoinPoint에서 Job 이름을 추출합니다.
     * JobLauncher.run(Job job, JobParameters jobParameters) 메소드의
     * 첫 번째 파라미터인 Job 객체에서 이름을 가져옵니다.
     *
     * @param joinPoint AOP JoinPoint 객체
     * @return Job 이름 또는 UNKNOWN_JOB
     */
    private String extractJobName(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        if (args == null || args.length == 0) {
            log.warn("JobLauncher.run 메소드에 파라미터가 없습니다.");
            return UNKNOWN_JOB;
        }

        Object firstArg = args[0];
        if (!(firstArg instanceof Job job)) {
            log.warn("JobLauncher.run 메소드의 첫 번째 파라미터가 Job 타입이 아닙니다. 실제 타입: {}",
                    firstArg != null ? firstArg.getClass().getSimpleName() : "null");
            return UNKNOWN_JOB;
        }

        String jobName = job.getName();

        if (jobName.trim().isEmpty()) {
            log.warn("Job 이름이 null 이거나 비어있습니다.");
            return UNKNOWN_JOB;
        }

        return jobName.trim();
    }
}