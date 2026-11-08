package com.emergencyconnectuae.simulation;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;

public class ConcurrentAssignmentSimulation {

    private static final String ASSIGNMENT_URL = "https://localhost:8080/api/assignments";

    // Paste your dispatcher JWT token here before running
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbjJAZWN1YWUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzgwMjIwMzAyLCJleHAiOjE3ODAyMjM5MDJ9.FQPZs1Ens-q_SvQ3PP-Bp810Dq-iya1MG-rHAAPATFZGy0tQukPce7pSX7FXhV2FV8bkqiBzV0VQlbjcAx52Tw";

    public static void main(String[] args) throws InterruptedException {
    	
    	UnsafeSslHelper.trustSelfSignedCertificatesForLocalTesting();

        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        System.out.println("Starting concurrent assignment simulation...");
        System.out.println("10 dispatchers will try to assign the SAME resource at the SAME time.");

        for (int i = 1; i <= numberOfThreads; i++) {

            int dispatcherNumber = i;

            executorService.submit(() -> {
                try {
                    RestTemplate restTemplate = new RestTemplate();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(JWT_TOKEN);

                    String body = """
                            {
                              "incidentId": 7702,
                              "resourceId": 107,
                              "assignedByUserId": 7
                            }
                            """;

                    HttpEntity<String> request = new HttpEntity<>(body, headers);

                    ResponseEntity<String> response =
                            restTemplate.postForEntity(ASSIGNMENT_URL, request, String.class);

                    System.out.println("Dispatcher " + dispatcherNumber
                            + " SUCCESS: " + response.getStatusCode());

                } catch (Exception e) {
                    System.out.println("Dispatcher " + dispatcherNumber
                            + " FAILED: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        System.out.println("Simulation completed.");
        System.out.println("Expected result: ONLY ONE request should succeed. Others should fail with 409 Conflict.");
    }
}