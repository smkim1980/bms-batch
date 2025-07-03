package tmoney.gbi.bmsbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BmsBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(BmsBatchApplication.class, args);
	}

}
