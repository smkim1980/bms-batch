package tmoney.gbi.bmsbatch.jobs.sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class SampleJobConfig {


    private final JobRepository jobRepository;

    @Qualifier("transactionManager")
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job sampleJob() {
        return new JobBuilder("sampleJob" ,jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(sampleStep())
                .build();
    }

    @Bean
    @JobScope
    public Step sampleStep() {
        return new StepBuilder("sampleStep" , jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("############################ Executing sample step>>>>>>>>>>");
                    return RepeatStatus.FINISHED;
                },platformTransactionManager)
                .build();
    }
}
