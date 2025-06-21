package tmoney.tnz.bmsbatch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import tmoney.tnz.bmsbatch.domain.Person;
import tmoney.tnz.bmsbatch.listener.FileCleanupListener;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BmsTxtFileJob {
    private final JobRepository jobRepository;
    @Qualifier("transactionManager") // 비즈니스 트랜잭션 매니저를 명시적으로 주입
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;

    public static final String JOB_NAME = "bmsTxtFile";

    @Bean(JOB_NAME)
    public Job personJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(personStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    @JobScope
    public Step personStep() {
        return new StepBuilder("personStep", jobRepository)
                .<Person, Person>chunk(10, transactionManager)
                .reader(multiFileReader(null, null, null)) // @Value 주입을 위해 null로 초기화
                .writer(personMyBatisBatchItemWriter())
                .listener(new FileCleanupListener())
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<Person> multiFileReader(
            @Value("#{jobParameters['requestDate']}") String requestDate,
            @Value("${batch.job.bms-txt-file.input-path}") String inputPath, // application.yml에서 경로 주입
            @Value("#{stepExecution}") StepExecution stepExecution) { // StepExecution 주입

        log.info("Job parameter 'requestDate': {}", requestDate);
        log.info("Reading files from path: {}", inputPath);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("file:" + inputPath + "*.txt");
        } catch (IOException e) {
            log.error("Failed to find file resources.", e);
            throw new RuntimeException("Failed to find file resources.", e);
        }

        // ExecutionContext에 리소스 정보를 저장하여 Listener가 참조하도록 함
        stepExecution.getExecutionContext().put("processedResources", resources);

        return new MultiResourceItemReaderBuilder<Person>()
                .name("multiFileReader")
                .resources(resources)
                .delegate(flatFileReader())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Person> flatFileReader() {
        return new FlatFileItemReaderBuilder<Person>()
                .name("flatFileReader")
                .linesToSkip(0)
                .delimited()
                .delimiter(",")
                .names("id", "name", "date")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Person.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public MyBatisBatchItemWriter<Person> personMyBatisBatchItemWriter() {
        return new MyBatisBatchItemWriterBuilder<Person>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("tmoney.tnz.bmsbatch.mapper.PersonMapper.insert")
                .build();
    }
}