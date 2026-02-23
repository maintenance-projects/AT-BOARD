package kr.co.ultari.at_board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtBoardApplication.class, args);
	}

}
