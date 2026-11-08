package com.emergencyconnectuae.controller;

import com.emergencyconnectuae.dto.ErrorResponse;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/security")
public class SecureFileController {

    @GetMapping("/file-check")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<?> checkFilePath(@RequestParam String path) {

        if (path.contains("..") || path.contains("/") || path.contains("\\") || path.contains("%2e")) {
            ErrorResponse error = new ErrorResponse(
                    400,
                    "Bad Request",
                    "Directory traversal attempt blocked. Raw file paths are not allowed.",
                    LocalDateTime.now()
            );

            return ResponseEntity.badRequest().body(error);
        }

        return ResponseEntity.ok("Path accepted safely. No filesystem access is performed.");
    }
}