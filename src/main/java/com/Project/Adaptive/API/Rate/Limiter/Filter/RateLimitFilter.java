package com.Project.Adaptive.API.Rate.Limiter.Filter;


import com.Project.Adaptive.API.Rate.Limiter.Repository.ApiKeyRepository;
import com.Project.Adaptive.API.Rate.Limiter.Service.ApiKeyService;
import com.Project.Adaptive.API.Rate.Limiter.dto.RateLimitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.Project.Adaptive.API.Rate.Limiter.Service.RateLimiterService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService){
        this.rateLimiterService = rateLimiterService;
    }

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException,ServletException{
        log.info("Filter Triggered");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader("X-API-Key");
        String ip = httpRequest.getRemoteAddr();

        String clientId;

        if(apiKey != null && !apiKey.isBlank() && apiKeyService.isValidKey(apiKey)){
            clientId = apiKey;
        }
        else{
            clientId = ip;
        }
        log.info("Request | ClientID: {} | ip: {} ",clientId,ip);

        boolean allowed = rateLimiterService.isAllowed(clientId);

        if(!allowed){
            try {
                httpResponse.setStatus(429);
                RateLimitResponse rateLimitResponse = new RateLimitResponse(
                        clientId,
                        "Too Many Requests, Retry shortly",
                        "Rate_Limited"
                );
                httpResponse.getWriter().write(
                        new ObjectMapper().writeValueAsString(rateLimitResponse)
                );
            } catch( Exception e){
                httpResponse.setStatus(429);
                httpResponse.getWriter().write("Too Many Requests, Retry Shortly");
            }
            log.warn("BLOCKED | clientID: {} | ip: {}",clientId,ip);
            return;
        }
        chain.doFilter(request,response);
        log.info("ALLOWED | clientID: {} | ip: {}",clientId,ip);
    }
}
