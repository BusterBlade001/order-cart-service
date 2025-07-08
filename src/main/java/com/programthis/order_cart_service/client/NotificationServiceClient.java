package com.programthis.order_cart_service.client;

import com.programthis.order_cart_service.dto.NotificationRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component // Marca esta clase como un componente de Spring para que pueda ser inyectada
public class NotificationServiceClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceBaseUrl;

    @Autowired // Inyecta RestTemplate y la URL del notification-service
    public NotificationServiceClient(RestTemplate restTemplate,
                                       @Value("${notification-service.url}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        // La URL base para el servicio de notificaciones (ej. http://localhost:8085)
        // Se añade "/api/v1" porque es el prefijo de los controladores en notification-service
        this.notificationServiceBaseUrl = notificationServiceUrl + "/api/v1"; 
    }

    /**
     * Envía una notificación por correo electrónico al Notification Service.
     *
     * @param requestDto Los detalles de la notificación a enviar (email, asunto, cuerpo del mensaje, tipo).
     * @return true si la notificación se envió correctamente (2xx status), false si hubo un error 4xx/5xx.
     * @throws RuntimeException Si ocurre un error inesperado de comunicación.
     */
    public boolean sendEmailNotification(NotificationRequestDto requestDto) {
        String url = notificationServiceBaseUrl + "/notifications/email"; // Endpoint completo para enviar emails
        try {
            // Usa postForObject para enviar el RequestDto. Como no esperamos un objeto de vuelta,
            // podemos simplemente obtener la respuesta como String o Void, o solo verificar el status.
            restTemplate.postForObject(url, requestDto, String.class); // Enviamos y recibimos un String como respuesta.
            System.out.println("Solicitud de notificación por email enviada a " + requestDto.getRecipientEmail());
            return true;
        } catch (HttpClientErrorException ex) {
            // Captura errores HTTP (4xx, 5xx) del servicio de notificaciones
            System.err.println("Error del Notification Service al enviar email a " + requestDto.getRecipientEmail() + ": " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            return false; // Indica fallo
        } catch (Exception ex) {
            // Captura otros errores (ej. problemas de conexión, IOException)
            System.err.println("Error al comunicarse con Notification Service para enviar email a " + requestDto.getRecipientEmail() + ": " + ex.getMessage());
            throw new RuntimeException("Error en comunicación con Notification Service", ex); // Relanza para que el servicio llamador pueda manejarlo
        }
    }

    // Podrías añadir otros métodos aquí si el Notification Service tuviera más tipos de notificación (SMS, Push, etc.)
}