package tmoney.gbi.bmsbatch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobRunner {

    private final JobLauncher jobLauncher;

    /**
     * 배치 Job을 실행합니다.
     * @param job 실행할 Job 객체
     * @param jobIdentifier Job을 식별하고 JobParameter를 생성하는 데 사용될 문자열
     */
    public void runJob(Job job, String jobIdentifier) {
        try {
            // JobParameter를 사용하여 매번 새로운 JobInstance를 생성합니다.
            // Job 식별자와 현재 시간을 파라미터로 넘겨주어 중복 실행을 방지합니다.
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobIdentifier", jobIdentifier)
                    .addString("requestTime", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);

            log.info("'{}' (Identifier: {}) Job has been successfully launched at {}",
                    job.getName(), jobIdentifier, LocalDateTime.now());

        } catch (Exception e) {
            log.error("Failed to launch '{}' Job (Identifier: {})", job.getName(), jobIdentifier, e);
        }
    }
}
