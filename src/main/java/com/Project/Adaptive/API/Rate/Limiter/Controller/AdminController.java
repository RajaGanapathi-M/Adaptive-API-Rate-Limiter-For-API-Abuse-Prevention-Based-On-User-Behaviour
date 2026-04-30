package com.Project.Adaptive.API.Rate.Limiter.Controller;

import com.Project.Adaptive.API.Rate.Limiter.Model.RatelimitStats;
import com.Project.Adaptive.API.Rate.Limiter.Service.RateLimiterService;
import com.Project.Adaptive.API.Rate.Limiter.Service.ApiKeyService;
import com.Project.Adaptive.API.Rate.Limiter.dto.ApiKeyResponse;
import com.Project.Adaptive.API.Rate.Limiter.dto.StatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.Project.Adaptive.API.Rate.Limiter.Repository.RateLimiterRepository;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private RateLimiterRepository repository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @GetMapping("/stats")
    public StatsResponse getStats(@RequestParam String clientId){
        if(!repository.exists(clientId)){
            return null;
        }
        RatelimitStats stats = rateLimiterService.getStats(clientId);
        return new StatsResponse(stats);
    }

    @PostMapping("/generate-key")
    public ApiKeyResponse generateKey(@RequestParam String clientName) {
        String apiKey = apiKeyService.generateKey(clientName);
        return new ApiKeyResponse(clientName,apiKey,"API Key generated successfully");
    }

    @DeleteMapping("/revoke-key")
    public ApiKeyResponse revokeKey(@RequestParam String apiKey){
        apiKeyService.revokeKey(apiKey);
        return new ApiKeyResponse(null,apiKey,"API Key revoked successfully");
    }
}
