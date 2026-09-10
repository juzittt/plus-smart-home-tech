package ru.yandex.practicum.order.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
public class RequestIdInterceptor  implements RequestInterceptor {

    private static final String HEADER_NAME = "X-Request-Id";

    @Override
    public void apply(RequestTemplate template) {
        String requestId = extractFromIncomingRequest()
                .orElseGet(this::generateNew);
        template.header(HEADER_NAME, requestId);
    }

    private Optional<String> extractFromIncomingRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        String value = attrs.getRequest().getHeader(HEADER_NAME);
        return value != null && !value.isBlank()
                ? Optional.of(value)
                : Optional.empty();
    }

    private String generateNew() {
        return UUID.randomUUID().toString();
    }
}
