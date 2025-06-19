package tmoney.tnz.bmsbatch.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    // --- 1. 비즈니스 로직용 DataSource 설정 (Primary) ---

    /**
     * application.yml의 spring.datasource 설정을 기반으로 기본 DataSource 속성을 생성합니다.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 비즈니스 로직(MyBatis)이 사용할 기본 DataSource를 생성합니다.
     * @Primary 어노테이션으로 이 DataSource가 기본값임을 명시합니다.
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    /**
     * 비즈니스 로직용 트랜잭션 매니저를 생성합니다.
     * @param dataSource @Primary로 지정된 기본 DataSource
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }


    // --- 2. Spring Batch 메타데이터용 DataSource 설정 ---

    /**
     * Spring Batch 메타데이터 전용 In-Memory H2 DataSource를 생성합니다.
     * 이 DataSource에는 Spring Batch 스키마가 자동으로 생성됩니다.
     */
    @Bean
    public DataSource batchDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("/org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    /**
     * 배치 메타데이터용 트랜잭션 매니저를 생성합니다.
     * @param batchDataSource 위에서 생성한 배치 전용 DataSource
     */
    @Bean
    public PlatformTransactionManager batchTransactionManager(@Qualifier("batchDataSource") DataSource batchDataSource) {
        return new DataSourceTransactionManager(batchDataSource);
    }


    // --- 3. Spring Batch 인프라 설정 ---

    /**
     * Spring Batch의 JobRepository를 생성합니다.
     * 이때, 배치 전용 DataSource와 트랜잭션 매니저를 명시적으로 주입합니다.
     * @param batchDataSource
     * @param batchTransactionManager
     */
    @Bean
    public JobRepository jobRepository(@Qualifier("batchDataSource") DataSource batchDataSource,
                                       @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(batchDataSource);
        factory.setTransactionManager(batchTransactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * Job을 실행할 JobLauncher를 생성합니다.
     * 위에서 생성한 jobRepository를 사용합니다.
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}