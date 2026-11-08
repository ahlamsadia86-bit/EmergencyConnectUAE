package com.emergencyconnectuae.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping({"/", "/login"})
    public String login() {
        return "login";
    }

    @GetMapping("/mfa")
    public String mfa() {
        return "mfa";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
    
    @GetMapping("/incidents")
    public String incidents() {
        return "incidents";
    }

    @GetMapping("/resources")
    public String resources() {
        return "resources";
    }
    
    @GetMapping("/dispatch")
    public String dispatch() {
        return "dispatch";
    }

    @GetMapping("/assignments")
    public String assignments() {
        return "assignments";
    }

    @GetMapping("/reservations")
    public String reservations() {
        return "reservations";
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return "audit-logs";
    }
    
    @GetMapping("/operations")
    public String operations() {
        return "operations";
    }
}