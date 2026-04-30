package com.Project.Adaptive.API.Rate.Limiter.Service;

import com.Project.Adaptive.API.Rate.Limiter.Repository.ApiKeyRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
public class ApiKeyService {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public String generateKey(String clientName){
        String apiKey = "rl-" + UUID.randomUUID().toString();
        apiKeyRepository.saveApiKey(apiKey,clientName);
        log.info("Key Generated | clientName: {} | apiKey: {}",clientName,apiKey);
        return apiKey;
    }

    public boolean isValidKey(String apiKey){
        return apiKeyRepository.isValidKey(apiKey);
    }

    public String getClientName(String apiKey){
        return apiKeyRepository.getClientName(apiKey);
    }

    public void revokeKey(String apiKey){
        apiKeyRepository.deleteApiKey(apiKey);
        log.warn("Key Revoked | apiKey: {}",apiKey);
    }
}
