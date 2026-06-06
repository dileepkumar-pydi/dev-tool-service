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
		return new OpenAPI().info(new Info().title("DevTool Service")
				.description("Developer helper APIs for encoding, decoding, and other utility workflows.")
				.version("1.0.0")
				.contact(new Contact().name("DevTool Team").email("dileepkumar26072022@gmail.com").url("")));
	}
}
