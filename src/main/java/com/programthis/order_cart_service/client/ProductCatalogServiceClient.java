package com.programthis.order_cart_service.client;

import com.programthis.order_cart_service.dto.ProductDto; // Crearemos este DTO
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate; // Ojo, RestTemplate es síncrono

import java.util.Optional;

@Component
public class ProductCatalogServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product-catalog-service.url}") // URL del Product Catalog Service desde application.properties
    private String productCatalogServiceUrl;

    public ProductCatalogServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<ProductDto> getProductById(Long productId) {
        try {
            // URL del endpoint del Product Catalog Service para obtener un producto por ID
            // Ej: http://localhost:8081/api/products/{productId}
            String url = productCatalogServiceUrl + "/api/products/" + productId;
            
            // Realiza la llamada HTTP GET y mapea la respuesta a ProductDto
            ProductDto product = restTemplate.getForObject(url, ProductDto.class);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            // Manejo de errores: el producto no existe, servicio no disponible, etc.
            System.err.println("Error al obtener producto del Product Catalog Service: " + e.getMessage());
            return Optional.empty();
        }
    }
}