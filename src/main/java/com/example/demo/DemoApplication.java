package com.example.demo;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
	@Bean
	public Clock appClock(@Value("${APP_TIMEZONE:Europe/Paris}") String timezone) {
		return Clock.system(ZoneId.of(timezone));
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(
			System.getenv().getOrDefault("APP_TIMEZONE", "Europe/Paris")
		));
		SpringApplication.run(DemoApplication.class, args);
	}

}
