import org.junit.jupiter.api.*;
import uk.ac.ed.inf.OrderProcessingHandler;
import uk.ac.ed.inf.OrderValidator;
import uk.ac.ed.inf.RestServiceClient;
import uk.ac.ed.inf.RouteCalculator;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemLoadTests {
    private static final String BASE_URL = "https://ilp-rest-2024.azurewebsites.net";
    private static final String RESULTS_DIR = "resultfiles";
    private OrderProcessingHandler orderProcessor;
    private RestServiceClient client;
    private RouteCalculator routeCalculator;
    private AtomicInteger successfulRequests;
    private AtomicInteger failedRequests;

    @BeforeAll
    void setUp() {
        orderProcessor = new OrderProcessingHandler(BASE_URL);
        client = new RestServiceClient(BASE_URL);
        routeCalculator = new RouteCalculator(BASE_URL);
        successfulRequests = new AtomicInteger(0);
        failedRequests = new AtomicInteger(0);
        new File(RESULTS_DIR).mkdirs();
    }

    @Nested
    @DisplayName("Order Processing Load Tests")
    class OrderProcessingLoadTests {

        @Test
        @DisplayName("Process multiple dates concurrently")
        void testConcurrentDateProcessing() throws InterruptedException {
            int numThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            List<String> dates = List.of(
                    "2025-01-23",
                    "2025-01-24",
                    "2025-01-25",
                    "2025-01-26",
                    "2025-01-27"
            );

            long startTime = System.currentTimeMillis();

            List<Future<?>> futures = new ArrayList<>();
            for (String date : dates) {
                futures.add(executor.submit(() -> {
                    try {
                        orderProcessor.processDayOrders(date);
                        successfulRequests.incrementAndGet();
                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            latch.await(2, TimeUnit.MINUTES);
            long duration = System.currentTimeMillis() - startTime;


            for (Future<?> future : futures) {
                assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
            }


            for (String date : dates) {
                verifyOutputFiles(date);
            }

            System.out.printf("Processed %d dates in %d ms (avg %d ms per date)%n",
                    dates.size(), duration, duration / dates.size());
            System.out.printf("Successful requests: %d, Failed requests: %d%n",
                    successfulRequests.get(), failedRequests.get());

            executor.shutdown();
        }

        @Test
        @DisplayName("Stress test order validation")
        void testOrderValidationUnderLoad() throws InterruptedException {
            int numThreads = 10;
            int requestsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            Restaurant[] restaurants = client.getRestaurants();
            Order[] sampleOrders = client.getOrders("2024-01-15");

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < requestsPerThread; j++) {
                            for (Order order : sampleOrders) {
                                OrderValidator validator = new OrderValidator();
                                validator.validateOrder(order, restaurants);
                                successfulRequests.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.MINUTES));
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Processed %d order validations in %d ms%n",
                    numThreads * requestsPerThread * sampleOrders.length, duration);
            System.out.printf("Average time per validation: %d ms%n",
                    duration / (numThreads * requestsPerThread * sampleOrders.length));

            executor.shutdown();
        }

        @Test
        @DisplayName("Test system recovery under load")
        void testSystemRecovery() throws InterruptedException {
            int numThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            // Process same date multiple times concurrently
            String testDate = "2024-01-15";
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        orderProcessor.processDayOrders(testDate);
                        successfulRequests.incrementAndGet();
                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });

                // Add slight delay
                Thread.sleep(100);
            }

            latch.await(2, TimeUnit.MINUTES);
            long duration = System.currentTimeMillis() - startTime;

            verifyOutputFiles(testDate);
            System.out.printf("Processed %d concurrent requests in %d ms%n",
                    numThreads, duration);

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Route Calculator Load Tests")
    class RouteCalculatorLoadTests {

        @Test
        @DisplayName("Test concurrent path calculations")
        void testConcurrentPathCalculation() throws InterruptedException {
            int numThreads = 10;
            int pathsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            LngLat startPoint = new LngLat(-3.186874, 55.944494);
            Restaurant[] restaurants = client.getRestaurants();

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < pathsPerThread; j++) {
                            for (Restaurant restaurant : restaurants) {
                                List<LngLat> path = routeCalculator.findPath(
                                        startPoint,
                                        restaurant.location(),
                                        false
                                );
                                assertNotNull(path);
                                assertFalse(path.isEmpty());
                                successfulRequests.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.MINUTES));
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Calculated %d paths in %d ms%n",
                    numThreads * pathsPerThread * restaurants.length, duration);
            System.out.printf("Average time per path: %d ms%n",
                    duration / (numThreads * pathsPerThread * restaurants.length));

            executor.shutdown();
        }

        @Test
        @DisplayName("Test path calculation with cache under load")
        void testPathCalculationWithCache() throws InterruptedException {
            int numThreads = 5;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            LngLat startPoint = new LngLat(-3.186874, 55.944494);
            LngLat endPoint = new LngLat(-3.188873, 55.944728);


            routeCalculator.findPath(startPoint, endPoint, false);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < iterationsPerThread; j++) {
                            List<LngLat> path = routeCalculator.findPath(
                                    startPoint,
                                    endPoint,
                                    false
                            );
                            assertNotNull(path);
                            assertFalse(path.isEmpty());
                            successfulRequests.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failedRequests.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(2, TimeUnit.MINUTES));
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Processed %d cached path requests in %d ms%n",
                    numThreads * iterationsPerThread, duration);
            System.out.printf("Average time per request: %d ms%n",
                    duration / (numThreads * iterationsPerThread));

            executor.shutdown();
        }
    }

    private void verifyOutputFiles(String date) {
        File deliveriesFile = new File(RESULTS_DIR, "deliveries-" + date + ".json");
        File flightpathFile = new File(RESULTS_DIR, "flightpath-" + date + ".json");

        assertTrue(deliveriesFile.exists(),
                "Deliveries file should exist for date: " + date);
        assertTrue(flightpathFile.exists(),
                "Flightpath file should exist for date: " + date);

        try {
            String deliveriesContent = Files.readString(deliveriesFile.toPath());
            String flightpathContent = Files.readString(flightpathFile.toPath());

            assertTrue(deliveriesContent.startsWith("[") && deliveriesContent.endsWith("]"),
                    "Deliveries file should contain valid JSON array");
            assertTrue(flightpathContent.startsWith("[") && flightpathContent.endsWith("]"),
                    "Flightpath file should contain valid JSON array");
        } catch (Exception e) {
            fail("Failed to verify output files: " + e.getMessage());
        }
    }
}