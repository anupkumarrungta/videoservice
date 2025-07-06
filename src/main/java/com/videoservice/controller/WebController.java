package com.videoservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving the HTML interface.
 */
@Controller
public class WebController {
    
    /**
     * Serve the main upload interface.
     * 
     * @return The upload page
     */
    @GetMapping({"", "/", "/upload"})
    public String index() {
        return "index";
    }
} 