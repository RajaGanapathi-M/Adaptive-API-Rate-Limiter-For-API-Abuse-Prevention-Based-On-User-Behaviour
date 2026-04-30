package com.Project.Adaptive.API.Rate.Limiter.Limiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.Project.Adaptive.API.Rate.Limiter.Repository.RateLimiterRepository;

@Component
public class TokenBucket {

    @Autowired
    private RateLimiterRepository repository;

    @Autowired
    private AdaptiveMetrics adaptiveMetrics;

    public synchronized boolean tryConsume(String clientId){

        if(adaptiveMetrics.isWindowExpired(clientId)){
            double currentRefillRate = repository.getRefillRate(clientId);
            double newRefillRate = adaptiveMetrics.evaluate(clientId, currentRefillRate);
            repository.saveRefillRate(clientId,newRefillRate);
        }

        long now = System.currentTimeMillis();
        long lastRefillTime = repository.getLastRefillTime(clientId);
        double refillRate = repository.getRefillRate(clientId);
        double tokens = repository.getTokens(clientId);
        long capacity = repository.getCapacity(clientId);

        long elapsedTime = now-lastRefillTime;
        double elapsedSec = elapsedTime/1000.0;

        tokens = Math.min(capacity,tokens+(elapsedSec*refillRate));
        repository.saveLastRefillTime(clientId,now);

        if(tokens>=1){
            tokens --;
            repository.saveTokens(clientId,tokens);
            adaptiveMetrics.recordRequest(clientId,true);
            repository.refreshTTL(clientId);
            return true;
        }

        repository.saveTokens(clientId,tokens);
        adaptiveMetrics.recordRequest(clientId,false);
        repository.refreshTTL(clientId);
        return false;
    }

}
