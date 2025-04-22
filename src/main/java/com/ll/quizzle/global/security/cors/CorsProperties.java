package com.ll.quizzle.global.security.cors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "custom.cors")
public class CorsProperties {
    private List<String> allowedOrigins;
} 