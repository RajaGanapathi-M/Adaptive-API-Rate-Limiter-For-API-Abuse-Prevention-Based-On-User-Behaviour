package com.Project.Adaptive.API.Rate.Limiter.dto;

public class RateLimitResponse {
    private String clientId;
    private String message;
    private String status;

    public RateLimitResponse(String clientId,String message,String status){
        this.clientId = clientId;
        this.message = message;
        this.status = status;
    }

    public String getClientId() { return clientId; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
}
