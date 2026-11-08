package com.emergencyconnectuae.simulation;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;

public class ConcurrentReservationSimulation {

    private static final String URL =
            "https://localhost:8080/api/reservations";

    private static final String JWT_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbjJAZWN1YWUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzgwMjIwMzAyLCJleHAiOjE3ODAyMjM5MDJ9.FQPZs1Ens-q_SvQ3PP-Bp810Dq-iya1MG-rHAAPATFZGy0tQukPce7pSX7FXhV2FV8bkqiBzV0VQlbjcAx52Tw";

    private static final Long INCIDENT_ID = 7009L;
    private static final Long RESOURCE_ID = 142L;

    public static void main(String[] args) throws InterruptedException {

        UnsafeSslHelper.trustSelfSignedCertificatesForLocalTesting();

        int users = 10;

        ExecutorService executor = Executors.newFixedThreadPool(users);
        CountDownLatch latch = new CountDownLatch(users);

        System.out.println("Starting SAFE Redis-lock reservation simulation...");
        System.out.println("10 users will try to reserve the SAME limited hospital resource at the SAME time.");
        System.out.println("Expected safe behavior: only allowed reservations succeed until capacity is exhausted. Others fail with 409 Conflict.");

        for (int i = 1; i <= users; i++) {
            int userNumber = i;

            executor.submit(() -> {
                try {
                    RestTemplate restTemplate = new RestTemplate();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(JWT_TOKEN);

                    String body = """
                            {
                              "incidentId": %d,
                              "resourceId": %d,
                              "reservedQuantity": 1
                            }
                            """.formatted(INCIDENT_ID, RESOURCE_ID);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            URL,
                            new HttpEntity<>(body, headers),
                            String.class
                    );

                    System.out.println("User " + userNumber + " SAFE SUCCESS: " + response.getStatusCode());

                } catch (Exception e) {
                    System.out.println("User " + userNumber + " FAILED: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("Safe reservation simulation completed.");
        System.out.println("Expected result: reservations must not exceed available capacity.");
    }
}