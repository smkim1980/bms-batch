package tmoney.gbi.bmsbatch.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "tmoney.gbi.bmsbatch.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DataSourceConfig {

    // (기존 데이터소스 및 트랜잭션 매니저 설정은 동일하게 유지)
    // ...

    // 1. 비즈니스 로직용 메인 데이터 소스 (기존 코드 유지)
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    // 2. 스프링 배치 메타데이터 저장용 인메모리 데이터 소스 (기존 코드 유지)
    @Bean(name="metaDataSource")
    public DataSource metaDataSource() {
        EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
        return embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2)
                .addScript("/org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(applicationContext.getResources("classpath:mapper/*.xml"));
        sessionFactory.setTypeAliasesPackage("tmoney.gbi.bmsbatch.domain");

        setMybatisConfig(sessionFactory);

        return sessionFactory.getObject();
    }

    private static void setMybatisConfig(SqlSessionFactoryBean sessionFactory) {
        // --- 추가된 부분 ---
        // 1. MyBatis의 세부 설정을 위한 Configuration 객체 생성
        org.apache.ibatis.session.Configuration mybatisConfiguration = new org.apache.ibatis.session.Configuration();
        // 2. map-underscore-to-camel-case: true 설정
        mybatisConfiguration.setMapUnderscoreToCamelCase(true);
        // 3. jdbc-type-for-null: null 설정 (JDBCType.NULL 사용)
        mybatisConfiguration.setJdbcTypeForNull(JdbcType.NULL);

        // 4. SqlSessionFactoryBean에 설정 객체 주입
        sessionFactory.setConfiguration(mybatisConfiguration);
    }

    // (이하 JobRepository, JobLauncher 등 나머지 Bean 설정은 동일하게 유지)
    // ...
    // 3. 비즈니스 로직용 트랜잭션 매니저 (기존 코드 유지)
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // 4. 배치 메타데이터용 트랜잭션 매니저 (기존 코드 유지)
    @Bean(name = "metaTransactionManager")
    public PlatformTransactionManager metaTransactionManager(@Qualifier("metaDataSource") DataSource metaDataSource) {
        return new DataSourceTransactionManager(metaDataSource);
    }

    // 5. 배치 메타데이터용 JobRepository (기존 코드 유지)
    @Bean
    public JobRepository jobRepository(@Qualifier("metaDataSource") DataSource metaDataSource, @Qualifier("metaTransactionManager") PlatformTransactionManager metaTransactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(metaDataSource);
        factory.setTransactionManager(metaTransactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    // 6. JobExplorer 빈 (기존 코드 유지)
    @Bean
    public JobExplorer jobExplorer(
            @Qualifier("metaDataSource") DataSource metaDataSource,
            @Qualifier("metaTransactionManager") PlatformTransactionManager metaTransactionManager) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(metaDataSource);
        factory.setTransactionManager(metaTransactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    // 7. JobLauncher (기존 코드 유지)
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
