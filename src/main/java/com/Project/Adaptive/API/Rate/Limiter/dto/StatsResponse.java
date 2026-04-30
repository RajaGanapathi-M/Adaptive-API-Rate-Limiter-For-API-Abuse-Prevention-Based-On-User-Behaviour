package com.Project.Adaptive.API.Rate.Limiter.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.Project.Adaptive.API.Rate.Limiter.Model.RatelimitStats;

public class StatsResponse {
    private String clientId;
    private double liveTokens;
    private double refillRate;
    private double baseRefillRate;
    private int cleanWindowCount;
    private long totalRequest;
    private long rejectedRequest;
    private String lastRefillTime;
    private String windowStartTime;
    private String checkedAt;

    public StatsResponse(RatelimitStats stats){
        this.clientId = stats.getClientId();
        this.liveTokens = Math.round(stats.getTokens()*100.0)/100.0;
        this.refillRate = stats.getRefillRate();
        this.baseRefillRate = stats.getBaseRefillRate();
        this.cleanWindowCount = stats.getCleanWindowCount();
        this.totalRequest = stats.getTotalRequest();
        this.rejectedRequest = stats.getRejectedRequest();
        this.windowStartTime = toReadable(stats.getWindowStartTime());
        this.lastRefillTime = toReadable(stats.getLastRefillTime());
        this.checkedAt = toReadable(System.currentTimeMillis());
    }

    public final String toReadable(long epochMs){
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public String getClientId(){return clientId;}
    public double getliveTokens(){return liveTokens;}
    public double getRefillRate(){return refillRate;}
    public double getBaseRefillRate(){return baseRefillRate;}
    public int getCleanWindowCount(){ return cleanWindowCount;}
    public long getTotalRequest(){ return totalRequest;}
    public long getRejectedRequest(){return rejectedRequest;}
    public String getWindowStartTime(){return windowStartTime;}
    public String getLastRefillTime(){return lastRefillTime;}
    public String getCheckedAt(){ return checkedAt;}
}
