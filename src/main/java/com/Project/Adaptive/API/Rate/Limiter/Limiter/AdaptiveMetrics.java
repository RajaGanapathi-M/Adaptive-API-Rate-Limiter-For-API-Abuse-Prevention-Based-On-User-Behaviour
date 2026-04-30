package com.Project.Adaptive.API.Rate.Limiter.Limiter;

import com.Project.Adaptive.API.Rate.Limiter.Repository.RateLimiterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveMetrics {
    private static final Logger log = LoggerFactory.getLogger(AdaptiveMetrics.class);

    @Autowired
    private RateLimiterRepository repository;

    @Value("${rate.limiter.window-duration}")
    private long windowDuration;

    @Value("${rate.limiter.clean-windows-required}")
    private int cleanWindowsRequired ;

    @Value("${rate.limiter.floor-percent}")
    private double floorPercent ;

    @Value("${rate.limiter.recovery-factor}")
    private double recoveryFactor;

    public void recordRequest(String clientId, boolean allowed){
        long total = repository.getTotalRequest(clientId);
        repository.saveTotalRequest(clientId,total+1);
        if(!allowed){
            long rejected = repository.getRejectedRequest(clientId);
            repository.saveRejectedRequest(clientId,rejected+1)   ;
        }
    }

    public boolean isWindowExpired(String clientId){
        long windowStartTime = repository.getWindowStartTime(clientId);
        long elapsed = System.currentTimeMillis()-windowStartTime;
        return elapsed >= windowDuration;
    }

    public double evaluate(String clientId,double currentRefillRate){
        double baseRefillRate = repository.getBaseRefillRate(clientId);
        double floor = floorPercent*baseRefillRate;

        long totalRequests = repository.getTotalRequest(clientId);
        long rejectedRequests = repository.getRejectedRequest(clientId);
        if(totalRequests == 0){
            resetWindow(clientId);
            return currentRefillRate;
        }

        double rejectRatio = (double) rejectedRequests/totalRequests;

        if(rejectRatio > 0.0){
            double newRate =  Math.max(floor,currentRefillRate*(1-rejectRatio));
            log.warn("PENALTY | clientId: {} | rejectRatio: {} | oldRate: {} | newRate: {}",
                    clientId,
                    String.format("%.2f", rejectRatio),
                    String.format("%.2f", currentRefillRate),
                    String.format("%.2f", newRate));
            repository.saveCleanWindowCount(clientId,0);
            resetWindow(clientId);
            return newRate;
        }

        int cleanWindowCount = repository.getCleanWindowCount(clientId);
        if(cleanWindowCount < cleanWindowsRequired){
            cleanWindowCount++;
            repository.saveCleanWindowCount(clientId,cleanWindowCount);
        }
        resetWindow(clientId);

        if(cleanWindowCount >= cleanWindowsRequired){
            double newRate = Math.min(baseRefillRate,currentRefillRate*recoveryFactor);
            log.info("RECOVERY | clientId: {} | cleanWindows: {} | oldRate: {} | newRate: {}",
                    clientId,
                    cleanWindowCount,
                    String.format("%.2f", currentRefillRate),
                    String.format("%.2f", newRate));
            return newRate;
        }
        return currentRefillRate;
    }
    private void resetWindow(String clientId){
        repository.saveTotalRequest(clientId,0);
        repository.saveRejectedRequest(clientId,0);
        repository.saveWindowStartTime(clientId,System.currentTimeMillis());
    }
}
