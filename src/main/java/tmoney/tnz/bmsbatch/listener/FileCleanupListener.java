package tmoney.tnz.bmsbatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class FileCleanupListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("파일 정리 리스너 시작...");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Step이 성공적으로 완료된 경우에만 파일 삭제
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            Resource[] processedResources = (Resource[]) stepExecution.getExecutionContext().get("processedResources");

            if (processedResources != null) {
                Arrays.stream(processedResources).forEach(resource -> {
                    try {
                        File file = resource.getFile();
                        if (file.delete()) {
                            log.info("성공적으로 파일을 삭제했습니다: {}", file.getPath());
                        } else {
                            log.warn("파일 삭제에 실패했습니다: {}", file.getPath());
                        }
                    } catch (IOException e) {
                        log.error("파일에 접근 중 오류가 발생했습니다: " + resource.getFilename(), e);
                    }
                });
            }
        }
        // ExitStatus를 변경하지 않고 그대로 반환
        return stepExecution.getExitStatus();
    }
}
