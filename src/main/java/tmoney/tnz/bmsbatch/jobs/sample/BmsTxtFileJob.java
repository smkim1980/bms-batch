package tmoney.tnz.bmsbatch.jobs.sample;

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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import tmoney.tnz.bmsbatch.domain.SamplePersonDto;
import tmoney.tnz.bmsbatch.listener.FileCleanupListener;
import tmoney.tnz.bmsbatch.mapper.SampleMapper;
import tmoney.tnz.bmsbatch.properties.FileJobProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BmsTxtFileJob {
    private final JobRepository jobRepository;

    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final SampleMapper sampleMapper;

    @Qualifier("sampleFileJobProperties")
    private final FileJobProperties fileJobProperties; // 2. FileJobProperties 주입

    public static final String JOB_NAME = "bmsTxtFile";

    @Bean(JOB_NAME)
    public Job personJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(personStep())
                .next(logVerificationStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    @JobScope
    public Step personStep() {
        return new StepBuilder("personStep", jobRepository)
                .<SamplePersonDto, SamplePersonDto>chunk(10, transactionManager)
                .reader(multiFileReader(null, null)) // 파라미터가 줄어듦
                .writer(personMyBatisBatchItemWriter())
                .listener(new FileCleanupListener())
                .build();
    }

    @Bean
    @JobScope
    public Step logVerificationStep() {
        return new StepBuilder("logVerificationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("====================================================");
                    log.info("DB 검증 단계 시작: 저장된 데이터를 확인합니다.");
                    log.info("====================================================");

                    List<SamplePersonDto> people = sampleMapper.findAll();

                    if (people.isEmpty()) {
                        log.info("DB에 저장된 데이터가 없습니다.");
                    } else {
                        log.info("총 {}개의 데이터가 DB에 저장되어 있습니다.", people.size());
                        people.forEach(person -> log.info("  -> Person: {}", person));
                    }

                    log.info("====================================================");
                    log.info("DB 검증 단계 완료.");
                    log.info("====================================================");

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }


    @Bean
    @StepScope
    public MultiResourceItemReader<SamplePersonDto> multiFileReader(
            @Value("#{jobParameters['requestDate']}") String requestDate,
            // 3. @Value로 주입받던 inputPath 제거
            @Value("#{stepExecution}") StepExecution stepExecution) {

        log.info("Job parameter 'requestDate': {}", requestDate);
        // 4. fileJobProperties 객체에서 설정값 사용
        log.info("Reading files from path: {}", fileJobProperties.getInputPath());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            // 4. fileJobProperties 객체에서 설정값 사용
            resources = resolver.getResources("file:" + fileJobProperties.getInputPath() + fileJobProperties.getFilePattern());
        } catch (IOException e) {
            log.error("Failed to find file resources.", e);
            throw new RuntimeException("Failed to find file resources.", e);
        }

        List<String> resourcePaths = Arrays.stream(resources)
                .map(resource -> {
                    try {
                        return resource.getFile().getAbsolutePath();
                    } catch (IOException e) {
                        log.error("Could not get file path from resource: {}", resource.getFilename(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        stepExecution.getExecutionContext().put("processedResources", resourcePaths);

        return new MultiResourceItemReaderBuilder<SamplePersonDto>()
                .name("multiFileReader")
                .resources(resources)
                .delegate(flatFileReader())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SamplePersonDto> flatFileReader() {
        return new FlatFileItemReaderBuilder<SamplePersonDto>()
                .name("flatFileReader")
                .linesToSkip(0)
                .delimited()
                // 5. fileJobProperties 객체에서 설정값 사용
                .delimiter(fileJobProperties.getDelimiter())
                .names(fileJobProperties.getNames())
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(SamplePersonDto.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public MyBatisBatchItemWriter<SamplePersonDto> personMyBatisBatchItemWriter() {
        return new MyBatisBatchItemWriterBuilder<SamplePersonDto>()
                .sqlSessionFactory(sqlSessionFactory)
                // 6. fileJobProperties 객체에서 설정값 사용
                .statementId(fileJobProperties.getWriterStatementId())
                .build();
    }
}