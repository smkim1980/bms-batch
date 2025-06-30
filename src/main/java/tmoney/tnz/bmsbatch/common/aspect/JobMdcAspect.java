package tmoney.tnz.bmsbatch.common.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;

/**
 * AOP를 사용하여 Spring Batch Job 실행 전후로 MDC(Mapped Diagnostic Context)에
 * Job 이름을 설정하고 제거합니다. 이를 통해 Logback의 SiftingAppender가
 * Job별로 로그 파일을 동적으로 생성할 수 있도록 지원합니다.
 */
@Aspect
@Component
public class JobMdcAspect {

    private static final String JOB_NAME_MDC_KEY = "jobName";

    /**
     * JobLauncher의 run 메소드 실행을 Pointcut으로 지정합니다.
     * 이 메소드는 모든 Job 실행의 시작점입니다.
     */
    @Pointcut("execution(* org.springframework.batch.core.launch.JobLauncher.run(..))")
    public void jobLauncherRun() {}

    /**
     * JobLauncher.run 메소드가 실행되기 전에 호출됩니다.
     * @param joinPoint 프록시된 메소드에 대한 정보를 담고 있습니다.
     */
    @Before("jobLauncherRun()")
    public void beforeJob(JoinPoint joinPoint) {
        // run 메소드의 첫 번째 파라미터인 JobExecution 객체를 가져옵니다.
        // Spring Batch 5 이전 버전에서는 JobParameters가 첫번째 인자일 수 있으나,
        // 현재 프로젝트(Spring Boot 3.x)에서는 JobExecution이 JobLauncher에 의해 생성되어 전달됩니다.
        // 더 안정적인 방법은 JobExecution을 직접 파라미터로 받는 것입니다.
        // 하지만 JobLauncher.run의 시그니처는 Job과 JobParameters 이므로,
        // Job 이름을 Job 객체에서 직접 가져옵니다.
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof org.springframework.batch.core.Job) {
            org.springframework.batch.core.Job job = (org.springframework.batch.core.Job) args[0];
            MDC.put(JOB_NAME_MDC_KEY, job.getName());
        }
    }

    /**
     * JobLauncher.run 메소드가 성공적으로 끝나거나 예외로 종료된 후 항상 호출됩니다.
     */
    @After("jobLauncherRun()")
    public void afterJob() {
        // Job 실행이 끝나면 MDC에서 Job 이름을 제거합니다.
        MDC.remove(JOB_NAME_MDC_KEY);
    }
}
