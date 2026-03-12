package com.fooddelivery.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI foodDeliveryOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Food Delivery API")
                        .description("Backend API for Food Delivery Platform")
                        .version("1.0"));
    }
}
//http://localhost:8080/swagger-ui/index.html