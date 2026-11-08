package com.emergencyconnectuae.simulation;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;

public class UnsafeAssignmentSimulation {

	private static final String UNSAFE_ASSIGNMENT_URL =
	        "https://localhost:8080/api/unsafe-demo/assign-without-lock";

    // Paste your dispatcher JWT token here
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbjJAZWN1YWUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzgwMjIwMzAyLCJleHAiOjE3ODAyMjM5MDJ9.FQPZs1Ens-q_SvQ3PP-Bp810Dq-iya1MG-rHAAPATFZGy0tQukPce7pSX7FXhV2FV8bkqiBzV0VQlbjcAx52Tw";

    public static void main(String[] args) throws InterruptedException {
    	
    	UnsafeSslHelper.trustSelfSignedCertificatesForLocalTesting();

        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        System.out.println("Starting UNSAFE assignment simulation...");
        System.out.println("10 dispatchers will try to assign the SAME resource WITHOUT Redis lock.");
        System.out.println("Expected unsafe behavior: more than one assignment may succeed.");

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
                              "incidentId": 7002,
                              "resourceId": 117,
                              "assignedByUserId": 7
                            }
                            """;

                    HttpEntity<String> request = new HttpEntity<>(body, headers);

                    ResponseEntity<String> response =
                            restTemplate.postForEntity(UNSAFE_ASSIGNMENT_URL, request, String.class);

                    System.out.println("Dispatcher " + dispatcherNumber
                            + " UNSAFE SUCCESS: " + response.getStatusCode());

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

        System.out.println("Unsafe simulation completed.");
        System.out.println("If multiple requests succeeded, this demonstrates the race condition.");
    }
}