package f.gregory.monitoring.starter;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestMetricsInterceptor requestMetricsInterceptor;

    public WebMvcConfig(RequestMetricsInterceptor requestMetricsInterceptor) {
        this.requestMetricsInterceptor = requestMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor).addPathPatterns("/**");
    }
}


