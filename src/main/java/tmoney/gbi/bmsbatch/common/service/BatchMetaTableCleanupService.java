package tmoney.gbi.bmsbatch.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchMetaTableCleanupService {

    @Qualifier("metaDataSource")
    private final DataSource metaDataSource;

    /**
     * Spring Batch 메타데이터 테이블을 초기화합니다.
     */
    public void cleanup(JobExecution jobExecution) {
        log.info("Job [{}] finished. Initializing metadata tables.", jobExecution.getJobInstance().getJobName());

        try {
            // Spring Batch가 제공하는 H2용 drop 스크립트를 실행합니다.
            ResourceDatabasePopulator dropPopulator = new ResourceDatabasePopulator(
                    new ClassPathResource("/org/springframework/batch/core/schema-drop-h2.sql")
            );
            DatabasePopulatorUtils.execute(dropPopulator, metaDataSource);

            // Spring Batch가 제공하는 H2용 create 스크립트를 실행합니다.
            ResourceDatabasePopulator createPopulator = new ResourceDatabasePopulator(
                    new ClassPathResource("/org/springframework/batch/core/schema-h2.sql")
            );
            DatabasePopulatorUtils.execute(createPopulator, metaDataSource);

            log.info("Batch metadata tables have been re-initialized successfully.");

        } catch (Exception e) {
            log.error("Failed to re-initialize batch metadata tables.", e);
        }
    }
}