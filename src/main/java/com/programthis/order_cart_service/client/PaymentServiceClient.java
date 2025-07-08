package com.programthis.order_cart_service.client;

import com.programthis.order_cart_service.dto.PaymentRequestDto;
import com.programthis.order_cart_service.dto.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component // Marca esta clase como un componente de Spring para que pueda ser inyectada
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceBaseUrl;

    @Autowired // Inyecta RestTemplate (definido en RestTemplateConfig) y la URL del payment-service
    public PaymentServiceClient(RestTemplate restTemplate,
                                @Value("${payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        // La URL base para el servicio de pagos (ej. http://localhost:8084)
        // Se añade "/api/v1" porque es el prefijo de los controladores en payment-service
        this.paymentServiceBaseUrl = paymentServiceUrl + "/api/v1"; 
    }

    /**
     * Procesa un pago enviando una solicitud al Payment Service.
     *
     * @param requestDto Los detalles de la solicitud de pago (orderId, amount, paymentMethodDetails).
     * @return Un Optional que contiene el PaymentResponseDto si el pago se procesa con éxito,
     * o Optional.empty() si el Payment Service devuelve un error 4xx o 5xx.
     * @throws RuntimeException Si ocurre un error inesperado de comunicación.
     */
    public Optional<PaymentResponseDto> processPayment(PaymentRequestDto requestDto) {
        String url = paymentServiceBaseUrl + "/payments/process"; // Endpoint completo para procesar pagos
        try {
            // Usa postForObject para enviar el objeto RequestDto y recibir el objeto ResponseDto
            // RestTemplate lanzará una excepción si el status code es 4xx o 5xx
            PaymentResponseDto responseDto = restTemplate.postForObject(url, requestDto, PaymentResponseDto.class);
            return Optional.ofNullable(responseDto); // Envuelve la respuesta en un Optional
        } catch (HttpClientErrorException ex) {
            // Captura errores HTTP (4xx, 5xx) del servicio de pagos
            System.err.println("Error del Payment Service al procesar pago para Order ID " + requestDto.getOrderId() + ": " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            return Optional.empty(); // Retorna Optional.empty() para indicar que el pago no se pudo procesar como exitoso.
        } catch (Exception ex) {
            // Captura otros errores (ej. problemas de conexión)
            System.err.println("Error al comunicarse con Payment Service para procesar pago de Order ID " + requestDto.getOrderId() + ": " + ex.getMessage());
            throw new RuntimeException("Error en comunicación con Payment Service", ex); // Relanza para que el servicio llamador pueda manejarlo
        }
    }

    // Opcional: Podrías añadir otros métodos aquí si necesitas, por ejemplo, consultar el estado de un pago
    /**
     * Obtiene el estado de un pago por ID de orden desde el Payment Service.
     *
     * @param orderId El ID de la orden para la que se busca el pago.
     * @return Un Optional que contiene el PaymentResponseDto si se encuentra el pago, o Optional.empty() si no se encuentra.
     */
    public Optional<PaymentResponseDto> getPaymentStatusByOrderId(String orderId) {
        String url = paymentServiceBaseUrl + "/payments/status/order/{orderId}";
        try {
            PaymentResponseDto responseDto = restTemplate.getForObject(url, PaymentResponseDto.class, orderId);
            return Optional.ofNullable(responseDto);
        } catch (HttpClientErrorException.NotFound ex) {
            System.err.println("Pago no encontrado para Order ID " + orderId + " en el Payment Service.");
            return Optional.empty();
        } catch (Exception ex) {
            System.err.println("Error al comunicarse con Payment Service para obtener estado de pago de Order ID " + orderId + ": " + ex.getMessage());
            throw new RuntimeException("Error en comunicación con Payment Service", ex);
        }
    }
}