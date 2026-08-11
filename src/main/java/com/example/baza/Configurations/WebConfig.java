package com.example.baza.Configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * "app.upload-dir" papkasini (mahsulot rasmlari) /uploads/** orqali statik serve qiladi —
 * classpath:/static/ ning odatiy ishlashiga tegmaydi (bu shunchaki QO'SHIMCHA resource handler).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String joyi = new File(uploadDir).getAbsolutePath().replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + joyi);
    }
}
