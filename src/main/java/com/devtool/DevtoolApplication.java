package com.devtool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = {@Server(url = "/v1/dev-tool", description = "Default Server URL")})
public class DevtoolApplication {
	public static void main(String args[]) {
		SpringApplication.run(DevtoolApplication.class, args);
	}

}
