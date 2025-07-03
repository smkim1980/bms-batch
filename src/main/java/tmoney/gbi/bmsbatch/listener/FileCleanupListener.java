package tmoney.gbi.bmsbatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import java.io.File;
import java.util.List;

@Slf4j
public class FileCleanupListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("파일 정리 리스너 시작...");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            // --- 여기부터 수정 ---
            // ExecutionContext에서 Resource[] 대신 파일 경로의 List<String>을 가져옵니다.
            @SuppressWarnings("unchecked")
            List<String> processedResourcePaths = (List<String>) stepExecution.getExecutionContext().get("processedResources");

            if (processedResourcePaths != null) {
                log.info("처리된 파일 {}개를 삭제합니다.", processedResourcePaths.size());
                processedResourcePaths.forEach(path -> {
                    File file = new File(path);
                    if (file.delete()) {
                        log.info("성공적으로 파일을 삭제했습니다: {}", file.getPath());
                    } else {
                        log.warn("파일 삭제에 실패했습니다: {}", file.getPath());
                    }
                });
            }
            // --- 여기까지 수정 ---
        }
        return stepExecution.getExitStatus();
    }
}