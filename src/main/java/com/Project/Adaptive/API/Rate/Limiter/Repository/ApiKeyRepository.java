package com.Project.Adaptive.API.Rate.Limiter.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.lang.invoke.StringConcatFactory;
import java.util.concurrent.TimeUnit;

@Repository
public class ApiKeyRepository {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    private static final String Key_prefix = "apikeys:";

    @Value("${rate.limiter.ttl.hours}")
    private long ttlHours;

    public void saveApiKey(String apiKey,String clientName){
        redisTemplate.opsForValue().set(Key_prefix+apiKey,clientName,ttlHours, TimeUnit.HOURS);
    }

    public boolean isValidKey(String apiKey){
        boolean valid = Boolean.TRUE.equals(redisTemplate.hasKey(Key_prefix+apiKey));

        if(valid){
            redisTemplate.expire(Key_prefix+apiKey,ttlHours,TimeUnit.HOURS);
        }
        return valid;
    }

    public String getClientName(String apiKey){
        return redisTemplate.opsForValue().get(Key_prefix+apiKey);
    }

    public void deleteApiKey(String apiKey){
        redisTemplate.delete(Key_prefix+apiKey);
    }
}
