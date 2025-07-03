package tmoney.gbi.bmsbatch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//@Getter
//@Setter
@Configuration
@ConfigurationProperties(prefix = "sample.file.job.file-process")
public class SampleFileJobProperties  extends FileJobProperties{

}
