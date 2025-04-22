package com.ll.quizzle.global.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;

@Configuration
public class RedisConfig {

    @Getter
    public static class RedisKeys {
        public static final String ADMIN_SESSION_PREFIX = "ADMIN_SESSION:";
        public static final String TOKEN_PREFIX = "TOKEN:";
        public static final String REFRESH_TOKEN_PREFIX = "REFRESH_TOKEN:";
    }

    /**
     * Redis Pub/Sub 메시지를 수신하기 위한 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }


    /**
     * RedisTemplate 설정:
     * - key 및 hash key: 문자열
     * - value 및 hash value: GenericJackson2JsonRedisSerializer (타입정보 포함 직렬화)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 직렬화 설정
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    /**
     * 관리자 세션 키 생성
     */
    public static String getAdminSessionKey(String sessionId) {
        return RedisKeys.ADMIN_SESSION_PREFIX + sessionId;
    }

    /**
     * 토큰 키 생성
     */
    public static String getTokenKey(String token) {
        return RedisKeys.TOKEN_PREFIX + token;
    }

    /**
     * 리프레시 토큰 키 생성
     */
    public static String getRefreshTokenKey(String refreshToken) {
        return RedisKeys.REFRESH_TOKEN_PREFIX + refreshToken;
    }

}
