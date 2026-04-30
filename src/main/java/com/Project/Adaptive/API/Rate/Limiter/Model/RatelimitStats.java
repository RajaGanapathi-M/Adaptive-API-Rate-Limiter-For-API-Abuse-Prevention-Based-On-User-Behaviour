package com.Project.Adaptive.API.Rate.Limiter.Model;

public class RatelimitStats {
    private String clientId;
    private double tokens;
    private double refillRate;
    private double baseRefillRate;
    private int cleanWindowCount;
    private long lastRefillTime;
    private long windowStartTime;
    private long totalRequest;
    private long rejectedRequest;

    public RatelimitStats(String clientId,double tokens,double refillRate,double baseRefillRate,
                          int cleanWindowCount,long windowStartTime,long lastRefillTime,long totalRequest,
                          long rejectedRequest){

        this.clientId = clientId;
        this.tokens = tokens;
        this.refillRate = refillRate;
        this.baseRefillRate = baseRefillRate;
        this.cleanWindowCount = cleanWindowCount;
        this.lastRefillTime = lastRefillTime;
        this.windowStartTime = windowStartTime;
        this.rejectedRequest = rejectedRequest;
        this.totalRequest = totalRequest;
    }

    public String getClientId(){ return clientId;}
    public double getTokens(){ return tokens; }
    public double getRefillRate(){ return refillRate;}
    public double getBaseRefillRate(){ return baseRefillRate;}
    public int getCleanWindowCount(){return cleanWindowCount;}
    public long getLastRefillTime(){ return lastRefillTime; }
    public long getWindowStartTime(){return windowStartTime;}
    public long getTotalRequest(){return totalRequest;}
    public long getRejectedRequest(){return rejectedRequest;}
}
