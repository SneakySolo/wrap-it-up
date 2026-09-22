package com.wrapitup.wrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@SpringBootApplication
public class WrapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WrapServiceApplication.class, args);
    }

//    /**
//     * Configure RedisTemplate for caching Wrap results.
//     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        StringRedisSerializer stringSerializer = new StringRedisSerializer();
//        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
//
//        template.setKeySerializer(stringSerializer);
//        template.setValueSerializer(jsonSerializer);
//        template.setHashKeySerializer(stringSerializer);
//        template.setHashValueSerializer(jsonSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
}