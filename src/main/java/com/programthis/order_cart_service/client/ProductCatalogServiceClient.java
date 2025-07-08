package com.programthis.order_cart_service.client;

import com.programthis.order_cart_service.dto.ProductDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class ProductCatalogServiceClient {

    private final RestTemplate restTemplate;
    private final String productCatalogBaseUrl;

    @Autowired
    public ProductCatalogServiceClient(RestTemplate restTemplate,
                                       @Value("${product-catalog-service.url}") String productCatalogServiceUrl) {
        this.restTemplate = restTemplate;
        // La URL base para el servicio de catálogo (ej. http://localhost:8081)
        // Se añade "/api" porque es el prefijo de los controladores en product-catalog-service.
        this.productCatalogBaseUrl = productCatalogServiceUrl + "/api"; 
    }

    /**
     * Obtiene los detalles de un producto del Product Catalog Service por su ID.
     * Utiliza RestTemplate para realizar la llamada HTTP GET.
     *
     * @param productId El ID del producto a buscar.
     * @return Un Optional que contiene el ProductDto si se encuentra el producto, o Optional.empty() si no se encuentra (404 Not Found).
     * @throws RuntimeException Si ocurre un error inesperado al comunicarse con el servicio (ej. error de conexión, 5xx).
     */
    public Optional<ProductDto> getProductById(Long productId) {
        String url = productCatalogBaseUrl + "/products/{id}"; // Construye la URL completa para el endpoint de producto
        try {
            // Realiza la llamada GET. RestTemplate deserializa automáticamente la respuesta JSON a ProductDto.
            // Si el servidor devuelve un 4xx o 5xx, RestTemplate lanzará una excepción.
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class, productId);
            return Optional.ofNullable(productDto); // Envuelve el DTO en un Optional.of() si no es null.
        } catch (HttpClientErrorException.NotFound ex) {
            // Captura específicamente las excepciones 404 Not Found (producto no encontrado).
            System.err.println("Producto con ID " + productId + " no encontrado en el Product Catalog Service.");
            return Optional.empty(); // Retorna un Optional vacío para indicar que el producto no existe.
        } catch (Exception ex) {
            // Captura cualquier otra excepción (ej. problemas de red, errores 5xx del servidor).
            System.err.println("Error al comunicarse con Product Catalog Service para obtener el producto " + productId + ": " + ex.getMessage());
            // Relanza una RuntimeException para que los servicios que llaman a este método puedan manejar el fallo.
            throw new RuntimeException("Error en comunicación con Product Catalog Service", ex);
        }
    }
}