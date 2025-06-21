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
                .reader(multiFileReader(null, null, null))
                .writer(personMyBatisBatchItemWriter())
                .listener(new FileCleanupListener())
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<Person> multiFileReader(
            @Value("#{jobParameters['requestDate']}") String requestDate,
            @Value("${batch.job.bms-txt-file.input-path}") String inputPath,
            @Value("#{stepExecution}") StepExecution stepExecution) {

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

        // --- 여기부터 수정 ---
        // Resource 객체는 직렬화할 수 없으므로, 파일 경로(String)의 리스트로 변환하여 저장합니다.
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

        // 직렬화 가능한 파일 경로 리스트를 ExecutionContext에 저장합니다.
        stepExecution.getExecutionContext().put("processedResources", resourcePaths);
        // --- 여기까지 수정 ---


        return new MultiResourceItemReaderBuilder<Person>()
                .name("multiFileReader")
                .resources(resources) // ItemReader 자체는 Resource 객체를 그대로 사용합니다.
                .delegate(flatFileReader())
                .build();
    }

    // ... (flatFileReader, personMyBatisBatchItemWriter 메서드는 변경 없음)
    @Bean
    @StepScope
    public FlatFileItemReader<Person> flatFileReader() {
        // REASON: Builder 패턴을 사용하여 가독성 및 설정의 명확성 향상
        return new FlatFileItemReaderBuilder<Person>()
                .name("flatFileReader")
                .linesToSkip(0) // 첫 줄부터 읽음
                .delimited() // 구분자 기반
                .delimiter(",") // 구분자는 콤마
                .names("id", "name", "date") // 필드 이름 설정
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Person.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public MyBatisBatchItemWriter<Person> personMyBatisBatchItemWriter() {
        // REASON: 단일 insert가 아닌 batch insert를 사용하여 DB 성능 최적화
        return new MyBatisBatchItemWriterBuilder<Person>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("tmoney.tnz.bmsbatch.mapper.PersonMapper.insert") // 매퍼의 쿼리 ID
                .build();
    }
}