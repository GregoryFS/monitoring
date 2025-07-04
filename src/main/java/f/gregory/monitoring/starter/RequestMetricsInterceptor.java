package f.gregory.monitoring.starter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;

public class RequestMetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry registry;
    private final ApplicationContext applicationContext;

    public static final ThreadLocal<Long> REQUEST_START_TIME = new ThreadLocal<>();

    public RequestMetricsInterceptor(MeterRegistry registry, ApplicationContext applicationContext) {
        this.registry = registry;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        REQUEST_START_TIME.set(System.nanoTime());

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String methodName = handlerMethod.getMethod().getName();
            String className = handlerMethod.getBeanType().getSimpleName();
            String requestUri = request.getRequestURI();

            String path = getPathPattern(request, handler);
            if (path == null) {
                path = requestUri;
            }

            Counter.builder("http.requests.total")
                    .description("Total number of HTTP requests processed")
                    .tag("method", request.getMethod())
                    .tag("path", path)
                    .tag("handler_class", className)
                    .tag("handler_method", methodName)
                    .register(registry)
                    .increment();
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Ничего не делаем здесь для этой задачи
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        REQUEST_START_TIME.remove(); // Очищаем ThreadLocal
    }

    private String getPathPattern(HttpServletRequest request, Object handler) {
        try {
            Object pathAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (pathAttribute instanceof String) {
                return (String) pathAttribute;
            }
        } catch (Exception e) {
            System.err.println("Error getting path pattern from request attributes: " + e.getMessage());
        }
        return null;
    }
}