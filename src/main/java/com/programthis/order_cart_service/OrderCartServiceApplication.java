package com.programthis.order_cart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Importar Bean
import org.springframework.web.client.RestTemplate; // Importar RestTemplate

@SpringBootApplication
public class OrderCartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderCartServiceApplication.class, args);
    }

    // Configuración para que Spring cree y gestione una instancia de RestTemplate
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}