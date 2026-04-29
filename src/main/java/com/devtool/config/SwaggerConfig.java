package com.devtool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI usersMicroserviceOpenAPI() {
		return new OpenAPI().info(new Info().title("DevTool Service").description("3Idiots Service").version("ONO")
				.contact(new Contact().name("3Idiots").email("vijaypopuri2021@gmail.com").url("")));
	}
}
