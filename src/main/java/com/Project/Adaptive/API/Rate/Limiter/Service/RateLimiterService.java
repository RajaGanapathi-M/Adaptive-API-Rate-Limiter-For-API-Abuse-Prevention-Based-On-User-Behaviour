package com.Project.Adaptive.API.Rate.Limiter.Service;


import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Project.Adaptive.API.Rate.Limiter.Limiter.TokenBucket;
import com.Project.Adaptive.API.Rate.Limiter.Model.RatelimitStats;
import com.Project.Adaptive.API.Rate.Limiter.Repository.RateLimiterRepository;

@Service
public class RateLimiterService {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    @Autowired
    private RateLimiterRepository repository;

    @Autowired
    private TokenBucket tokenBucket;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    @Value("${rate.limiter.capacity}")
    private long capacity;

    @Value("${rate.limiter.refill-rate}")
    private double refillRate;

    @Value("${rate.limiter.window-duration}")
    private long windowDuration;

    @Value("${rate.limiter.clean-windows-required}")
    private int cleanWindowsRequired;

    @Value("${rate.limiter.recovery-factor}")
    private double recoveryFactor;

    @Value("${rate.limiter.floor-percent}")
    private double floorPercent;

    public boolean isAllowed(String clientId){
        if(!repository.exists(clientId)){
            log.info("New Client | clientID : {}",clientId);
            repository.saveTokens(clientId,capacity);
            repository.saveRefillRate(clientId,refillRate);
            repository.saveBaseRefillRate(clientId,refillRate);
            repository.saveTotalRequest(clientId,0);
            repository.saveRejectedRequest(clientId,0);
            repository.saveCleanWindowCount(clientId,0);
            repository.saveLastRefillTime(clientId,System.currentTimeMillis());
            repository.saveWindowStartTime(clientId,System.currentTimeMillis());
        }
        return tokenBucket.tryConsume(clientId);
    }

    public RatelimitStats getStats(String clientId){
        double storedTokens = repository.getTokens(clientId);
        double refillRate = repository.getRefillRate(clientId);
        double baseRefillRate = repository.getBaseRefillRate(clientId);
        long capacity = repository.getCapacity(clientId);
        long lastRefillTime = repository.getLastRefillTime(clientId);
        long windowStartTime = repository.getWindowStartTime(clientId);
        long totalRequest = repository.getTotalRequest(clientId);
        long rejectedRequest = repository.getRejectedRequest(clientId);
        int cleanWindowCount = repository.getCleanWindowCount(clientId);

        long now  = System.currentTimeMillis();
        long elapsed = now-lastRefillTime;
        double elapsedSec = elapsed/1000.0;
        double liveTokens = Math.min(capacity,storedTokens+(refillRate*elapsedSec));

        return new RatelimitStats(
                clientId,
                liveTokens,
                refillRate,
                baseRefillRate,
                cleanWindowCount,
                windowStartTime,
                lastRefillTime,
                totalRequest,
                rejectedRequest
        );
    }
}
