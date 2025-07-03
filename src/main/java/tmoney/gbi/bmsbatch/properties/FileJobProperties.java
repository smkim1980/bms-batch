package tmoney.gbi.bmsbatch.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileJobProperties {
    /**
     * 처리할 파일이 있는 입력 디렉토리 경로
     */
    private String inputPath;
    /**
     * 처리할 파일의 이름 패턴 (e.g., "*.txt", "data-*.csv")
     */
    private String filePattern;
    /**
     * 파일 내 필드를 구분하는 구분자 (e.g., ",", "|")
     */
    private String delimiter;
    /**
     * 파일의 각 필드에 매핑될 객체의 필드명 (순서대로)
     */
    private String[] names;
    /**
     * MyBatis ItemWriter에서 사용할 statement ID
     */
    private String writerStatementId;
}
