package com.Project.Adaptive.API.Rate.Limiter.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RateLimiterRepository {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    private static final String Key_Prefix = "User:";

    private String key(String clientId){
        return Key_Prefix + clientId;
    }

    private void save(String clientId, String field, String value){
        redisTemplate.opsForHash().put(key(clientId),field,value);
    }

    private String get(String clientId, String field){
        Object value = redisTemplate.opsForHash().get(key(clientId),field);
        return (value != null) ? value.toString():null;
    }

    public boolean exists(String clientId){
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(clientId)));
    }

    public void saveTokens(String clientId,double tokens){
        save(clientId,"Tokens",String.valueOf(tokens));
    }

    public double getTokens(String clientId){
        String value = get(clientId,"Tokens");
        return (value != null) ? Double.parseDouble(value):0.0;
    }

    public void saveRefillRate(String clientId, double refillRate){
        save(clientId,"refillRate",String.valueOf(refillRate));
    }

    public double getRefillRate(String clientId){
        String value = get(clientId,"refillRate");
        return (value != null) ? Double.parseDouble(value):0.0;
    }

    public void saveLastRefillTime(String clientId, long lastRefillTime){
        save(clientId,"lastRefillTime",String.valueOf(lastRefillTime));
    }

    public long getLastRefillTime(String clientId){
        String value = get(clientId,"lastRefillTime");
        return (value!=null) ? Long.parseLong(value):System.currentTimeMillis();
    }

    public void saveBaseRefillRate(String clientId, double baseRefillRate){
        save(clientId,"baseRefillRate",String.valueOf(baseRefillRate));
    }

    public double getBaseRefillRate(String clientId){
        String value = get(clientId,"baseRefillRate");
        return (value != null) ? Double.parseDouble(value):0.0;
    }

    public void saveTotalRequest(String clientId, long count){
        save(clientId,"totalRequest",String.valueOf(count));
    }

    public long getTotalRequest(String clientId){
        String value = get(clientId,"totalRequest");
        return (value!=null) ? Long.parseLong(value):0;
    }

    public void saveRejectedRequest(String clientId, long count){
        save(clientId,"rejectedRequest",String.valueOf(count));
    }

    public long getRejectedRequest(String clientId){
        String value = get(clientId,"rejectedRequest");
        return (value!=null) ? Long.parseLong(value):0;
    }

    public void saveCleanWindowCount(String clientId, int count){
        save(clientId,"cleanWindowCount",String.valueOf(count));
    }

    public int getCleanWindowCount(String clientId){
        String value = get(clientId,"cleanWindowCount");
        return (value!=null) ? Integer.parseInt(value):0;
    }

    public void saveWindowStartTime(String clientId, long time){
        save(clientId,"WindowStartTime",String.valueOf(time));
    }

    public long getWindowStartTime(String clientId){
        String value = get(clientId,"WindowStartTime");
        return (value!=null) ? Long.parseLong(value):System.currentTimeMillis();
    }

    @Value("${rate.limiter.capacity}")
    private long capacity;

    public void saveCapacity(String clientId, long capacity){
        save(clientId,"capacity",String.valueOf(capacity));
    }

    public long getCapacity(String clientId){
        String value = get(clientId,"capacity");
        return (value!=null) ? Long.parseLong(value):capacity;
    }

    @Value("${rate.limiter.ttl.hours}")
    private long ttlhours;

    public void refreshTTL(String clientId){
        redisTemplate.expire("User:"+clientId,ttlhours, TimeUnit.HOURS);
    }

}
