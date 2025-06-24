package tmoney.tnz.bmsbatch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tmoney.tnz.bmsbatch.config.BatchJobRunner;

import java.time.LocalDateTime;

//@Slf4j
//@Component
//@RequiredArgsConstructor
public class SampleJobScheduler {

//    private final BatchJobRunner batchJobRunner;
//
//    @Qualifier("sampleJob")
//    private final Job sampleJob;
//
//    /**
//     * 매일 새벽 4시 30분에 sampleJob을 실행합니다.
//     * Cron Expression: 초 분 시 일 월 요일
//     * "0 30 4 * * ?" = 매일 4시 30분 0초에 실행
//     */
//    @Scheduled(cron = "0 30 4 * * ?")
//    public void runSampleJobAt430() {
//        batchJobRunner.runJob(sampleJob, "runSampleJobAt430");
//    }
//
//    /**
//     * 매일 새벽 5시 15분에 sampleJob을 실행합니다.
//     * Cron Expression: 초 분 시 일 월 요일
//     * "0 15 5 * * ?" = 매일 5시 15분 0초에 실행
//     */
//    @Scheduled(cron = "0 15 5 * * ?")
//    public void runSampleJobAt515() {
//        batchJobRunner.runJob(sampleJob, "runSampleJobAt515");
//    }

//    /**
//     * 10초(10000ms)에 한 번씩 sampleJob을 실행합니다.
//     * fixedRate: 이전 작업의 시작 시간으로부터 고정된 시간(밀리초)이 지난 후에 실행합니다.
//     */
//    @Scheduled(fixedRate = 10000)
//    public void runJobEvery10Seconds() {
//        // 공통 러너를 사용하여 Job 실행
//        batchJobRunner.runJob(sampleJob, "tenSecondJobTrigger");
//    }

//    /**
//     * 매 시간 정각에 sampleJob을 실행합니다.
//     * Cron Expression: 초 분 시 일 월 요일
//     * "0 0 * * * ?" = 매 시간 0분 0초에 실행
//     */
//    @Scheduled(cron = "0 0 * * * ?")
//    public void runJobHourly() {
//        // 공통 러너를 사용하여 Job 실행
//        batchJobRunner.runJob(sampleJob, "hourlyJobTrigger");
//    }
}