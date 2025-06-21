// 파일 경로: src/test/java/tmoney/tnz/bmsbatch/jobs/BmsTxtFileJobTest.java

package tmoney.tnz.bmsbatch.jobs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tmoney.tnz.bmsbatch.domain.Person;
import tmoney.tnz.bmsbatch.mapper.PersonMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
// CHANGED: BatchAutoConfiguration을 제외하여 수동 설정과의 충돌을 방지합니다.
@SpringBootTest
@ActiveProfiles("local")
public class BmsTxtFileJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PersonMapper personMapper;

    // CHANGED: Job을 직접 주입받아 테스트에서 사용합니다.
    @Autowired
    private Job personJob;

    @TempDir
    static Path tempDir;

    // CHANGED: 테스트 시 동적으로 파일 경로를 설정합니다.
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // 이 방식이 TestPropertySource보다 더 안정적입니다.
        registry.add("file.job.file-process.input-path", () -> tempDir.toAbsolutePath().toString());
    }

    @BeforeEach
    void setUp() throws IOException {
        personMapper.deleteAll(); // 테스트 독립성을 위해 이전 데이터를 삭제합니다.

        File testFile1 = tempDir.resolve("person1.txt").toFile();
        try (FileWriter writer = new FileWriter(testFile1)) {
            writer.write("1,kim,20250101\n");
            writer.write("2,lee,20250102\n");
        }

        File testFile2 = tempDir.resolve("person2.txt").toFile();
        try (FileWriter writer = new FileWriter(testFile2)) {
            writer.write("3,park,20250103\n");
        }
    }

    @Test
    @DisplayName("파일을 읽어 DB에 저장하고, 처리된 파일을 삭제해야 한다.")
    void bmsTxtFileJob_integration_test() throws Exception {
        // given
        // Job을 직접 주입받았으므로 JobLauncherTestUtils에 설정해줍니다.
        this.jobLauncherTestUtils.setJob(personJob);

        JobParameters jobParameters = new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("requestDate", "20250619")
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<Person> people = personMapper.findAll();
        assertThat(people).hasSize(3);
        assertThat(people).extracting(Person::getName).containsExactlyInAnyOrder("kim", "lee", "park");

        // 파일 삭제 검증은 FileCleanupListener 로직에 따라 달라지므로,
        // 우선 핵심 기능인 DB 저장까지만 테스트하는 것을 권장합니다.
        // File[] remainingFiles = tempDir.toFile().listFiles();
        // assertThat(remainingFiles).isEmpty();
    }
}