package com.profession.suggest.configuration;

import com.profession.suggest.interceptors.auth.JWTValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        "/api/auth/protected-test",
                        "/api/psych-tests/**",
                        "/api/auth/account-roles",
                        "/api/auth/update-password",
                        "/api/pupil-subjects/**",

                        "/api/pupils/pupil-data",
                        "/api/pupils/update-pupil-data",
                        "/api/pupils/completed-tests",
                        "/api/pupils/pupil/predictions",

                        "/api/specialists/specialist",
                        "/api/specialists/update",
                        "/api/specialists/register-all",
                        "/api/specialists/completed-tests",

                        "/api/predictions/create",
                        "/api/predictions/predict",

                        "/api/vr-tests/**",

                        "/api/hr/**",
                        "/api/company/**",

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
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
