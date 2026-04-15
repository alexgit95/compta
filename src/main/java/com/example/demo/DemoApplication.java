package com.example.demo;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(
			System.getenv().getOrDefault("APP_TIMEZONE", "Europe/Paris")
		));
		SpringApplication.run(DemoApplication.class, args);
	}

}
