package com.d1gaming.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.d1gaming.user.user")
public class GamingLeagueEventBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamingLeagueEventBackEndApplication.class, args);
	}

}
