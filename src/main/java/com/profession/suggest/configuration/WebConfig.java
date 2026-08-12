package com.profession.suggest.configuration;

import com.profession.suggest.interceptors.auth.JWTValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JWTValidationInterceptor jwtValidationInterceptor;
    @Value("${public.folder}")
    private String publicFolderPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtValidationInterceptor)
                .addPathPatterns(
                        "/api/auth/auto-register-all",
                        "/api/psych-tests/**",
                        "/api/auth/account-roles",
                        "/api/auth/update-password",
                        "/api/pupil-subjects/**",

                        "/api/pupils/**",

                        "/api/specialists",
                        "/api/specialists/specialist",
                        "/api/specialists/specialist/**",
                        "/api/specialists/register-all",
                        "/api/specialists/completed-tests",
                        "/api/specialists/reference-data",
                        "/api/specialists/professions/**",
                        "/api/specialists/professions-spheres/**",

                        "/api/predictions/**",

                        "/api/vr-tests/**",

                        "/api/company/**",

                        "/api/schools/**",
                        "/api/curators/**",

                        "/api/simulations/**",

                        "/api/comparison/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/public/**")
                .addResourceLocations("file:" + publicFolderPath)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
    @Bean
    public RestTemplate restTemplate(
            @Value("${prediction.service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${prediction.service.read-timeout-ms:180000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }
}
