package com.ll.quizzle.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 43200) // 12시간
public class SessionConfig extends AbstractHttpSessionApplicationInitializer {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setSameSite("None");
        serializer.setUseSecureCookie(true);
        serializer.setCookiePath("/");
        serializer.setCookieName("JSESSIONID");
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }

    @Bean
    public RedisOperations<String, Object> redisOperations(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
    
    @Bean(name = "customSessionRepository")
    public RedisSessionRepository sessionRepository(RedisOperations<String, Object> redisOperations) {
        RedisSessionRepository repository = new RedisSessionRepository(redisOperations);
        
        // 세션 타임아웃 시간 설정 (12시간)
        repository.setDefaultMaxInactiveInterval(Duration.ofHours(12));
        
        // 세션 만료 시 로깅
        log.info("Redis 세션 저장소가 구성되었습니다. 세션 만료 시간: {}시간", 12);
        
        return repository;
    }
} 