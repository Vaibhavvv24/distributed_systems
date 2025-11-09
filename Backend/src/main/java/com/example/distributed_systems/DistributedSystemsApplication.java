package com.example.distributed_systems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedSystemsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedSystemsApplication.class, args);
	}

}
