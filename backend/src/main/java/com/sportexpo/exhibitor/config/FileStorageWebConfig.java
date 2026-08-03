package com.sportexpo.exhibitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class FileStorageWebConfig implements WebMvcConfigurer {
    private final Path rootPath;
    private final String publicPrefix;

    public FileStorageWebConfig(@Value("${app.file-storage.root-path}") String rootPath,
                                @Value("${app.file-storage.public-prefix}") String publicPrefix) {
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
        this.publicPrefix = publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length() - 1) : publicPrefix;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(rootPath.toUri().toString());
    }
}
