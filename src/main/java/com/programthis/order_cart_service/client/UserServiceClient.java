package com.programthis.order_cart_service.client;

import com.programthis.order_cart_service.dto.UserDto; // Asegúrate de que este DTO exista
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component // Marca esta clase como un componente de Spring
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    @Autowired // Inyecta RestTemplate y la URL del user-service
    public UserServiceClient(RestTemplate restTemplate,
                               @Value("${user-service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        // La URL base para el servicio de usuarios (ej. http://localhost:8082)
        // Se añade "/api" porque es el prefijo de los controladores en user-service
        this.userServiceBaseUrl = userServiceUrl + "/api"; 
    }

    /**
     * Obtiene los detalles de un usuario del User Service por su ID.
     *
     * @param userId El ID del usuario a buscar.
     * @return Un Optional que contiene el UserDto si se encuentra el usuario, o Optional.empty() si no se encuentra (404 Not Found).
     * @throws RuntimeException Si ocurre un error inesperado de comunicación.
     */
    public Optional<UserDto> getUserById(Long userId) {
        String url = userServiceBaseUrl + "/users/{id}"; // Endpoint completo para obtener usuario por ID
        try {
            // Realiza la llamada GET. RestTemplate deserializa automáticamente la respuesta JSON a UserDto.
            UserDto userDto = restTemplate.getForObject(url, UserDto.class, userId);
            return Optional.ofNullable(userDto); // Envuelve el DTO en un Optional.of() si no es null.
        } catch (HttpClientErrorException.NotFound ex) {
            // Captura específicamente las excepciones 404 Not Found (usuario no encontrado).
            System.err.println("Usuario con ID " + userId + " no encontrado en el User Service.");
            return Optional.empty(); // Retorna un Optional vacío si el usuario no se encuentra.
        } catch (Exception ex) {
            // Captura cualquier otra excepción (ej. problemas de conexión, 5xx del servidor).
            System.err.println("Error al comunicarse con User Service para obtener el usuario " + userId + ": " + ex.getMessage());
            throw new RuntimeException("Error en comunicación con User Service", ex); // Relanza para que el servicio llamador pueda manejarlo
        }
    }

    // Podrías añadir otros métodos aquí si necesitas interactuar con más endpoints del User Service,
    // por ejemplo, para registrar nuevos usuarios, autenticar, etc.
}