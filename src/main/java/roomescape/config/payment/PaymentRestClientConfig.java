package roomescape.config.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class PaymentRestClientConfig {

    private final ObjectMapper objectMapper;

    private final TossPaymentConfigProperties tossProperties;

    public PaymentRestClientConfig(ObjectMapper objectMapper, TossPaymentConfigProperties tossProperties) {
        this.objectMapper = objectMapper;
        this.tossProperties = tossProperties;
    }

    @Bean
    public RestClient.Builder tossRestClientBuilder() {
        return RestClient.builder()
                .baseUrl(tossProperties.baseUri())
                .defaultHeaders(this::setTossHeaders)
                .defaultStatusHandler(new TossPaymentResponseErrorHandler(objectMapper))
                .requestFactory(tossRequestFactory());
    }

    private void setTossHeaders(HttpHeaders headers) {
        headers.setBasicAuth(tossAuthorization());
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private ClientHttpRequestFactory tossRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(tossProperties.connectTimeout());
        factory.setReadTimeout(tossProperties.readTimeout());
        return factory;
    }

    private String tossAuthorization() {
        String secretKeyWithoutPassword = tossProperties.secret() + ":";
        Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(secretKeyWithoutPassword.getBytes(StandardCharsets.UTF_8));
    }
}
