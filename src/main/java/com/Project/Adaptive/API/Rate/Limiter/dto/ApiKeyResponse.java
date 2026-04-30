package com.Project.Adaptive.API.Rate.Limiter.dto;

public class ApiKeyResponse {
    private String clientName;
    private String apiKey;
    private String message;

    public ApiKeyResponse(String clientName,String apiKey,String message){
        this.clientName = clientName;
        this.apiKey = apiKey;
        this.message = message;
    }

    public String getClientName(){return clientName;}
    public String getApiKey(){return apiKey;}
    public String getMessage(){return message;}
}
