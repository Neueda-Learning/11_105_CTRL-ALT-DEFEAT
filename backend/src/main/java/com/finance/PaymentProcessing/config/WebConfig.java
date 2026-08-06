package com.finance.PaymentProcessing.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers Spring Data's Pageable/Sort argument resolvers with Spring MVC.
 *
 * Without spring-boot-starter-data-jpa on the classpath the auto-configuration
 * that normally registers PageableHandlerMethodArgumentResolver is absent.
 * This configurer re-adds it so that controller parameters of type
 * {@code Pageable} are resolved from HTTP query params:
 *   ?page=0&size=20&sort=createdAt,desc
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        PageableHandlerMethodArgumentResolver pageableResolver =
            new PageableHandlerMethodArgumentResolver(sortResolver);
        pageableResolver.setMaxPageSize(100);
        resolvers.add(sortResolver);
        resolvers.add(pageableResolver);
    }
}
