package com.example.distributed_systems;

import org.springframework.boot.SpringApplication;

public class TestDistributedSystemsApplication {

	public static void main(String[] args) {
		SpringApplication.from(DistributedSystemsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
