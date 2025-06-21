package tmoney.tnz.bmsbatch.config;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class DataSourceConfig {

    // 1. 비즈니스 로직용 메인 데이터 소스 (기존 H2 서버)
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    // 2. 스프링 배치 메타데이터 저장용 인메모리 데이터 소스
    @Bean(name="metaDataSource")
    public DataSource metaDataSource() {
        EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
        return embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2)
                .addScript("/org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    // 3. 비즈니스 로직용 트랜잭션 매니저
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // 4. 배치 메타데이터용 트랜잭션 매니저
    @Bean(name = "metaTransactionManager")
    public PlatformTransactionManager metaTransactionManager(@Qualifier("metaDataSource") DataSource metaDataSource) {
        return new DataSourceTransactionManager(metaDataSource);
    }

    // 5. 배치 메타데이터용 JobRepository
    @Bean
    public JobRepository jobRepository(@Qualifier("metaDataSource") DataSource metaDataSource, @Qualifier("metaTransactionManager") PlatformTransactionManager metaTransactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(metaDataSource);
        factory.setTransactionManager(metaTransactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    // 6. JobExplorer 빈 (핵심 수정 사항)
    // TransactionManager를 주입받아 설정해주는 코드를 추가합니다.
    @Bean
    public JobExplorer jobExplorer(
            @Qualifier("metaDataSource") DataSource metaDataSource,
            @Qualifier("metaTransactionManager") PlatformTransactionManager metaTransactionManager) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(metaDataSource);
        factory.setTransactionManager(metaTransactionManager); // <-- 이 부분이 추가되었습니다.
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    // 7. JobLauncher
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}

