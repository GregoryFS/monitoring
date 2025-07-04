package f.gregory.monitoring.starter;


import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;


@Configuration
@ConditionalOnClass({MeterRegistry.class, RequestMappingInfoHandlerMapping.class})
public class MonitoringAutoConfiguration {

    static {
        System.out.println("[DEBUG] MonitoringAutoConfiguration: Autoconfiguration class loaded JVM.");
    }

    @Value("${spring.application.name:unknown-app}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags("application", applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestMetricsInterceptor requestMetricsInterceptor(MeterRegistry registry, ApplicationContext applicationContext) {
        return new RequestMetricsInterceptor(registry, applicationContext);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(RequestMetricsInterceptor requestMetricsInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(requestMetricsInterceptor).addPathPatterns("/**");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricResponseBodyAdvice metricResponseBodyAdvice(MeterRegistry registry) {
        return new MetricResponseBodyAdvice(registry);
    }
}
