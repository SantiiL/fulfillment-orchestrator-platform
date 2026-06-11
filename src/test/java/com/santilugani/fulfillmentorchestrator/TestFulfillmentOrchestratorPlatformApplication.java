package com.santilugani.fulfillmentorchestrator;

import org.springframework.boot.SpringApplication;

public class TestFulfillmentOrchestratorPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(FulfillmentOrchestratorPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
