package com.emergencyconnectuae.simulation;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

public class RealWorldEmergencyLoadSimulation {

    private static final String BASE_URL = "https://localhost:8080";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbjJAZWN1YWUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzc5OTcyMzExLCJleHAiOjE3Nzk5NzU5MTF9.a4Z5mVjP46fuyuOM8ZU2Y5kQob1wxtTEkVyWDWF6PNjK5E6XpIYVhOXebBoiISWgQNqObqaOxb2q7GvYP9Uknw";
    private static final Long USER_ID = 1L;

    private static final Random random = new Random();

    private static final List<Long> createdIncidentIds = new CopyOnWriteArrayList<>();
    private static final List<Long> createdResourceIds = new CopyOnWriteArrayList<>();
    private static final List<Long> hospitalBedResourceIds = new CopyOnWriteArrayList<>();

    private static final AtomicInteger success = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();

    private static final List<Long> latencies = new CopyOnWriteArrayList<>();

    private static final String[] INCIDENT_TITLES = {
            "Major Road Accident", "Apartment Fire", "Medical Emergency",
            "Flooded Road", "Gas Leak", "Metro Station Injury",
            "School Bus Accident", "Building Evacuation", "Heat Stroke Case",
            "Highway Vehicle Collision"
    };

    private static final String[] LOCATIONS = {
            "Abu Dhabi Corniche", "Dubai Marina", "Sharjah Industrial Area",
            "Al Ain City Center", "Sheikh Zayed Road", "Yas Island",
            "Khalifa City", "Jumeirah", "Mussafah", "Downtown Dubai"
    };

    private static final String[] PRIORITIES = {
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    };

    private static final String[] RESOURCE_TYPES = {
            "AMBULANCE", "FIRE_TRUCK", "POLICE_UNIT", "HOSPITAL_BED", "MEDICAL_TEAM"
    };

    public static void main(String[] args) throws InterruptedException {
    	
    	UnsafeSslHelper.trustSelfSignedCertificatesForLocalTesting();

        int totalRequests = 1000;
        int threadPoolSize = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        long start = System.currentTimeMillis();

        System.out.println("===== REAL-WORLD EMERGENCY LOAD SIMULATION STARTED =====");
        System.out.println("Total simulated operations: " + totalRequests);
        System.out.println("Concurrent workers: " + threadPoolSize);

        for (int i = 1; i <= totalRequests; i++) {
            int requestNumber = i;

            executor.submit(() -> {
                long operationStart = System.currentTimeMillis();

                try {
                    int operation = random.nextInt(100);

                    if (operation < 25) {
                        createRandomIncident(requestNumber);
                    } else if (operation < 40) {
                        createRandomResource(requestNumber);
                    } else if (operation < 55) {
                        queryAvailableResources();
                    } else if (operation < 70) {
                        assignRandomResource();
                    } else if (operation < 80) {
                        reserveHospitalCapacity();
                    } else if (operation < 90) {
                        updateRandomIncidentStatus();
                    } else if (operation < 96) {
                        readDashboard();
                    } else {
                        readAuditLogs();
                    }

                    success.incrementAndGet();

                } catch (Exception e) {
                    failed.incrementAndGet();
                    System.out.println("FAILED operation " + requestNumber + ": " + e.getMessage());
                } finally {
                    long operationEnd = System.currentTimeMillis();
                    latencies.add(operationEnd - operationStart);
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long end = System.currentTimeMillis();
        long totalTime = end - start;

        Collections.sort(latencies);

        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = percentile(latencies, 50);
        long p95 = percentile(latencies, 95);
        long p99 = percentile(latencies, 99);
        long min = latencies.isEmpty() ? 0 : latencies.get(0);
        long max = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);

        double throughput = (totalRequests * 1000.0) / totalTime;

        System.out.println("===== REAL-WORLD EMERGENCY LOAD SIMULATION COMPLETED =====");
        System.out.println("Successful operations: " + success.get());
        System.out.println("Failed/conflict operations: " + failed.get());
        System.out.println("Created incidents tracked: " + createdIncidentIds.size());
        System.out.println("Created resources tracked: " + createdResourceIds.size());
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        System.out.println("Min latency: " + min + " ms");
        System.out.println("Average latency: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("P50 latency: " + p50 + " ms");
        System.out.println("P95 latency: " + p95 + " ms");
        System.out.println("P99 latency: " + p99 + " ms");
        System.out.println("Max latency: " + max + " ms");
    }

    private static RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(JWT_TOKEN);
        return headers;
    }

    private static void createRandomIncident(int number) {
        String title = randomItem(INCIDENT_TITLES);
        String location = randomItem(LOCATIONS);
        String priority = randomItem(PRIORITIES);

        String body = """
                {
                  "title": "%s #%d",
                  "description": "Real-world simulated emergency requiring coordinated response",
                  "location": "%s",
                  "priority": "%s"
                }
                """.formatted(title, number, location, priority);

        ResponseEntity<Map> response = restTemplate().postForEntity(
                BASE_URL + "/api/incidents",
                new HttpEntity<>(body, headers()),
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object id = response.getBody().get("id");
            if (id instanceof Number n) {
                createdIncidentIds.add(n.longValue());
            }
        }
    }

    private static void createRandomResource(int number) {
        String type = randomItem(RESOURCE_TYPES);
        String location = randomItem(LOCATIONS);

        int capacity = type.equals("HOSPITAL_BED") ? random.nextInt(10) + 1 : 1;

        String body = """
                {
                  "resourceName": "%s Unit #%d",
                  "type": "%s",
                  "location": "%s",
                  "capacity": %d
                }
                """.formatted(type, number, type, location, capacity);

        ResponseEntity<Map> response = restTemplate().postForEntity(
                BASE_URL + "/api/resources",
                new HttpEntity<>(body, headers()),
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object id = response.getBody().get("id");
            if (id instanceof Number n) {
                Long resourceId = n.longValue();
                createdResourceIds.add(resourceId);

                if (type.equals("HOSPITAL_BED")) {
                    hospitalBedResourceIds.add(resourceId);
                }
            }
        }
    }

    private static void queryAvailableResources() {
        restTemplate().exchange(
                BASE_URL + "/api/resources/available?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                String.class
        );
    }

    private static void assignRandomResource() {
        if (createdIncidentIds.isEmpty() || createdResourceIds.isEmpty()) {
            queryAvailableResources();
            return;
        }

        Long incidentId = randomFromList(createdIncidentIds);
        Long resourceId = randomFromList(createdResourceIds);

        String body = """
                {
                  "incidentId": %d,
                  "resourceId": %d,
                  "assignedByUserId": %d
                }
                """.formatted(incidentId, resourceId, USER_ID);

        restTemplate().postForEntity(
                BASE_URL + "/api/assignments",
                new HttpEntity<>(body, headers()),
                String.class
        );
    }

    private static void reserveHospitalCapacity() {
        if (createdIncidentIds.isEmpty() || hospitalBedResourceIds.isEmpty()) {
            queryAvailableResources();
            return;
        }

        Long incidentId = randomFromList(createdIncidentIds);
        Long resourceId = randomFromList(hospitalBedResourceIds);
        int quantity = random.nextInt(3) + 1;

        String body = """
                {
                  "incidentId": %d,
                  "resourceId": %d,
                  "reservedQuantity": %d
                }
                """.formatted(incidentId, resourceId, quantity);

        restTemplate().postForEntity(
                BASE_URL + "/api/reservations",
                new HttpEntity<>(body, headers()),
                String.class
        );
    }

    private static void updateRandomIncidentStatus() {
        if (createdIncidentIds.isEmpty()) {
            readDashboard();
            return;
        }

        Long incidentId = randomFromList(createdIncidentIds);
        String status = random.nextBoolean() ? "IN_PROGRESS" : "RESOLVED";

        String body = """
                {
                  "status": "%s"
                }
                """.formatted(status);

        restTemplate().exchange(
                BASE_URL + "/api/incidents/" + incidentId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers()),
                String.class
        );
    }

    private static void readDashboard() {
        restTemplate().exchange(
                BASE_URL + "/api/dashboard/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                String.class
        );

        restTemplate().exchange(
                BASE_URL + "/api/dashboard/active-incidents",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                String.class
        );
    }

    private static void readAuditLogs() {
        restTemplate().exchange(
                BASE_URL + "/api/audit-logs?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                String.class
        );
    }

    private static String randomItem(String[] array) {
        return array[random.nextInt(array.length)];
    }

    private static Long randomFromList(List<Long> list) {
        return list.get(random.nextInt(list.size()));
    }

    private static long percentile(List<Long> sortedLatencies, int percentile) {
        if (sortedLatencies.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.size()) - 1;
        return sortedLatencies.get(Math.max(0, Math.min(index, sortedLatencies.size() - 1)));
    }
}