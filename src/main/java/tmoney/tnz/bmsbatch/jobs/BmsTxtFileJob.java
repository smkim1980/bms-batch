package tmoney.tnz.bmsbatch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory; // REASON: MyBatisBatchItemWriter에 필요

    // REASON: Job의 이름을 상수로 관리하여 명확성과 재사용성 증대
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
                .reader(multiFileReader(null)) // REASON: @Value 주입을 위해 null로 초기화
                .writer(personMyBatisBatchItemWriter())
                .listener(new FileCleanupListener()) // ADDED: 파일 삭제를 위한 리스너 등록
                .build();
    }

    @Bean
    @StepScope // REASON: Job 파라미터나 외부 설정값을 받아 동적으로 리더를 생성하기 위함
    public MultiResourceItemReader<Person> multiFileReader(
            @Value("#{jobParameters['requestDate']}") String requestDate) { // 예시 JobParameter

        log.info("Job a parameter 'requestDate': {}", requestDate);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            // REASON: 하드코딩된 경로 대신 application.yml의 설정값을 사용
            resources = resolver.getResources("file:" + "/Users/smkim/intellij/bms_batch" + "/" + "*.txt");
        } catch (IOException e) {
            log.error("파일 리소스를 찾지 못했습니다.", e);
            throw new RuntimeException("파일 리소스를 찾지 못했습니다.", e);
        }

        // REASON: reader가 읽은 리소스 정보를 listener와 공유하기 위해 ExecutionContext에 저장
        // StepExecution은 StepScope 빈에 자동으로 주입됩니다.
        // 이 예제에서는 명시적으로 주입받지 않았지만, 필요시 @Value("#{stepExecution}") StepExecution stepExecution 로 주입 가능합니다.
        // 여기서는 리스너에서 stepExecution.getExecutionContext()를 통해 직접 접근합니다.
        // 주의: 이 방식은 리스너가 Step의 ExecutionContext에 접근할 수 있기 때문에 가능합니다.
        // MultiResourceItemReader가 내부적으로 읽은 리소스 목록을 저장하고,
        // 이를 리스너에서 접근할 수 있는 표준적인 방법은 아래와 같이 ExecutionContext를 사용하는 것입니다.
        // 다만, MultiResourceItemReader 자체는 읽은 리소스 목록을 직접 노출하지 않으므로,
        // 리스너에서 삭제할 파일 목록을 알기 위해서는 reader를 생성할 때 사용한 resource 목록을 ExecutionContext에 넣어줘야 합니다.
        // StepContext에 리소스 정보를 저장
        // 리스너에서 이 정보를 사용
        // 이 부분은 개선이 필요할 수 있습니다. 리스너가 리소스 목록을 직접 알 수 있도록 구조를 변경하는 것이 좋습니다.
        // 예를 들어, StepExecutionListener가 리소스 목록을 직접 주입받도록 설정할 수 있습니다.
        // 하지만 여기서는 간단하게 리스너가 reader와 동일한 경로를 보도록 수정하겠습니다.
        // FileCleanupListener도 경로를 주입받도록 수정하는 것이 더 좋습니다.

        return new MultiResourceItemReaderBuilder<Person>()
                .name("multiFileReader")
                .resources(resources)
                .delegate(flatFileReader())
                .build();
    }

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
