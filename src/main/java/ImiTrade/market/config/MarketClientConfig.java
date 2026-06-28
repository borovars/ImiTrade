package ImiTrade.market.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Registers the MOEX integration beans and enables Spring scheduling.
 *
 * <p>Bound properties: {@link MarketProperties} (MOEX ISS endpoint) and
 * {@link SchedulerProperties} (price-refresh cadence). {@link EnableScheduling} lives
 * here so the whole market-data feature (client + service + scheduler) is wired in one
 * place rather than on the application entry point.
 *
 * <p>MOEX serves {@code .json} responses with {@code Content-Type: text/plain}
 * (see the ISS developer guide). The default Jackson converter only reads
 * {@code application/json}, so a second {@link MappingJackson2HttpMessageConverter}
 * that also accepts {@code text/plain} is registered — otherwise the response body
 * cannot be decoded into the DTOs. The request URI is assembled entirely in
 * {@code MoexClient} (no {@code baseUrl} here), keeping the client testable with
 * {@code MockRestServiceServer}.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({MarketProperties.class, SchedulerProperties.class})
public class MarketClientConfig {

    @Bean
    public RestClient moexRestClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter textPlainJsonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        textPlainJsonConverter.setSupportedMediaTypes(
                List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));

        ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        return builder
                .requestFactory(requestFactory)
                .messageConverters(converters -> converters.add(textPlainJsonConverter))
                .build();
    }
}
