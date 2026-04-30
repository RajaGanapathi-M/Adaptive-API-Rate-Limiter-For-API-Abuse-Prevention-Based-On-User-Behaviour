package com.Project.Adaptive.API.Rate.Limiter.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/hello")
    public String hello(){
        return "Hello, Your Request Passed Down the Rate Limiter";
    }
}
