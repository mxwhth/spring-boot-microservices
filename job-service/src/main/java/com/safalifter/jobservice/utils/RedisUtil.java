package com.safalifter.jobservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safalifter.jobservice.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    public String get(String key) {
        return findObject(key, String.class);
    }

    public void set(String key, String value) {
        set(key, value, Duration.ofMinutes(10));
    }

    public void set(String key, String value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public <T> T findObject(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, clazz);
        } catch (JsonProcessingException e) {
            logger.error("json covert error: {}", e.getMessage(), e);
        }
        return null;
    }

    public <T> void saveObject(String key, T object) {
        saveObject(key, object, Duration.ofMinutes(10));
    }

    public <T> void saveObject(String key, T object, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(object), duration);
        } catch (JsonProcessingException e) {
            logger.error("json covert error: {}", e.getMessage(), e);
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
