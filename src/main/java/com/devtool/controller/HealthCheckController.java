package com.devtool.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("health-check")
public class HealthCheckController {
    
    @GetMapping()
    public String healthcheck() {        
        return "Ono Click service is up and running";
    }

}
