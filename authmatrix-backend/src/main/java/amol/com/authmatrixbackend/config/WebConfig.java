package amol.com.authmatrixbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // This ensures React handles all routes that aren't mapped to backend APIs
        registry.addViewController("/{spring:(?!api|static).*}")
                .setViewName("forward:/index.html");
        registry.addViewController("/**/{spring:(?!api|static).*}")
                .setViewName("forward:/index.html");
    }
}

