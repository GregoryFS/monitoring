package f.gregory.monitoring.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.concurrent.TimeUnit;

@ControllerAdvice
public class MetricResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetricResponseBodyAdvice(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getParameterType().isPrimitive() && !returnType.getParameterType().equals(String.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        String requestUri = request.getURI().getPath();

        String resultCode = "UNKNOWN";
        String resultDescription = "UNKNOWN";

        if (body != null && selectedContentType != null && selectedContentType.includes(MediaType.APPLICATION_JSON)) {
            try {
                if (body instanceof f.gregory.monitoring.starter.DTO.Result) {
                    f.gregory.monitoring.starter.DTO.Result resultDto = (f.gregory.monitoring.starter.DTO.Result) body;
                    resultCode = resultDto.getCode() != null ? resultDto.getCode() : "NULL_CODE";
                    resultDescription = resultDto.getDescription() != null ? resultDto.getDescription() : "NULL_DESCRIPTION";
                } else {
                    JsonNode rootNode = objectMapper.valueToTree(body);
                    JsonNode resultNode = rootNode.get("result");

                    if (resultNode != null && resultNode.isObject()) {
                        JsonNode codeNode = resultNode.get("code");
                        JsonNode descriptionNode = resultNode.get("description");
                        if (codeNode != null && codeNode.isTextual()) {
                            resultCode = codeNode.asText();
                        }
                        if (descriptionNode != null && descriptionNode.isTextual()) {
                            resultDescription = descriptionNode.asText();
                        }
                    } else if (rootNode != null && rootNode.isObject() && rootNode.has("code") && rootNode.has("description")) {
                        JsonNode codeNode = rootNode.get("code");
                        JsonNode descriptionNode = rootNode.get("description");
                        if (codeNode != null && codeNode.isTextual()) {
                            resultCode = codeNode.asText();
                        }
                        if (descriptionNode != null && descriptionNode.isTextual()) {
                            resultDescription = descriptionNode.asText();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Error parsing response body for metrics in ResponseBodyAdvice for " + requestUri + ": " + e.getMessage());
            }
        }

        Long startTime = RequestMetricsInterceptor.REQUEST_START_TIME.get();

        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            String path = request.getURI().getPath();
            String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";


            Timer.builder("http.requests.duration")
                    .description("Длительность HTTP-запросов")
                    .tag("method", method)
                    .tag("path", path)
//                    .tag("status", httpStatus)
                    .tag("result_code", resultCode)
                    .tag("result_description", resultDescription)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);

            Counter.builder("http.requests.total_detailed")
                    .description("Общее количество HTTP-запросов по методу, пути, статусу и кастомному результату")
                    .tag("method", method)
                    .tag("path", path)
//                    .tag("status", httpStatus)
                    .tag("result_code", resultCode)
                    .tag("result_description", resultDescription)
                    .register(meterRegistry)
                    .increment();
        } else {
            if (!"/actuator/prometheus".equals(requestUri)) {
                System.err.println("[ERROR] MetricResponseBodyAdvice.beforeBodyWrite: Starting time  ThreadLocal = NULL " + requestUri + ". The metrics for this query may be incomplete.");
            }
        }

        return body;
    }
}