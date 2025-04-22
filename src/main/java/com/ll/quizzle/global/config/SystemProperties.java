package com.ll.quizzle.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "system")
public class SystemProperties {
	private String systemEmail;
	private String systemPasswordHash;
	private String secondaryPasswordHash;
	private List<String> allowedOrigins;
}
