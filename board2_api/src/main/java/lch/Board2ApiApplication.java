package lch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Board2ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(Board2ApiApplication.class, args);
	}

}
