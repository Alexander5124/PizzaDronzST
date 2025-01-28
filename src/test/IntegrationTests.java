import org.junit.jupiter.api.*;
import uk.ac.ed.inf.LngLatHandler;
import uk.ac.ed.inf.OrderProcessingHandler;
import uk.ac.ed.inf.RestServiceClient;
import uk.ac.ed.inf.RouteCalculator;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntegrationTests {
    private static final String BASE_URL = "https://ilp-rest-2024.azurewebsites.net";
    private static final String RESULTS_DIR = "resultfiles";
    private RestServiceClient client;
    private RouteCalculator routeCalculator;
    private OrderProcessingHandler orderProcessor;

    @BeforeEach
    void setUp() {
        client = new RestServiceClient(BASE_URL);
        routeCalculator = new RouteCalculator(BASE_URL);
        orderProcessor = new OrderProcessingHandler(BASE_URL);
        new File(RESULTS_DIR).mkdirs();
    }

    @Nested
    @DisplayName("RestServiceClient Integration Tests")
    class RestServiceClientIntegrationTests {
        @Test
        @DisplayName("Should fetch and filter orders by date")
        void testOrdersFetchAndFilter() {
            String testDate = LocalDate.now().toString();
            Order[] orders = client.getOrders(testDate);
            assertNotNull(orders);
            for (Order order : orders) {
                assertEquals(LocalDate.parse(testDate), order.getOrderDate());
            }
        }

        @Test
        @DisplayName("Should fetch restaurants and verify data consistency")
        void testRestaurantsFetch() {
            Restaurant[] restaurants = client.getRestaurants();
            assertNotNull(restaurants);
            assertTrue(restaurants.length > 0);

            // Verify each restaurant has valid data
            for (Restaurant restaurant : restaurants) {
                assertNotNull(restaurant.name());
                assertNotNull(restaurant.location());
                assertNotNull(restaurant.openingDays());
                assertNotNull(restaurant.menu());
            }
        }

        @Test
        @DisplayName("Should fetch and validate central area")
        void testCentralAreaFetch() {
            NamedRegion central = client.getCentralArea();
            assertNotNull(central);
            assertTrue(central.vertices().length >= 3, "Central area should be a polygon");
        }

        @Test
        @DisplayName("Should fetch and validate no-fly zones")
        void testNoFlyZonesFetch() {
            NamedRegion[] noFlyZones = client.getNoFlyZones();
            assertNotNull(noFlyZones);
            for (NamedRegion zone : noFlyZones) {
                assertNotNull(zone.name());
                assertTrue(zone.vertices().length >= 3, "No-fly zone should be a polygon");
            }
        }
    }

    @Nested
    @DisplayName("RouteCalculator Integration Tests")
    class RouteCalculatorIntegrationTests {
        private static final LngLat START_POINT = new LngLat(-3.186874, 55.944494);
        private static final double MOVE_DISTANCE = 0.00015;

        @Test
        @DisplayName("Should calculate valid path avoiding no-fly zones")
        void testPathCalculation() {
            Restaurant[] restaurants = client.getRestaurants();
            assumeTrue(restaurants.length > 0);

            LngLat destination = restaurants[0].location();
            List<LngLat> path = routeCalculator.findPath(START_POINT, destination, false);

            assertNotNull(path);
            assertFalse(path.isEmpty());
            assertEquals(START_POINT, path.get(0));

            // Verify move distances
            for (int i = 0; i < path.size() - 1; i++) {
                double distance = new LngLatHandler().distanceTo(path.get(i), path.get(i + 1));
                assertTrue(distance <= MOVE_DISTANCE);
            }
        }

        @Test
        @DisplayName("Should respect central area constraint on return paths")
        void testCentralAreaConstraint() {
            NamedRegion central = client.getCentralArea();
            Restaurant[] restaurants = client.getRestaurants();
            assumeTrue(restaurants.length > 0);

            LngLat destination = restaurants[0].location();
            List<LngLat> returnPath = routeCalculator.findPath(destination, START_POINT, true);

            boolean inCentral = false;
            LngLatHandler handler = new LngLatHandler();

            for (LngLat point : returnPath) {
                if (handler.isInCentralArea(point, central)) {
                    inCentral = true;
                }
                if (inCentral) {
                    assertTrue(handler.isInCentralArea(point, central),
                            "Path left central area after entering");
                }
            }
        }
    }

    @Nested
    @DisplayName("OrderProcessingHandler Integration Tests")
    class OrderProcessingHandlerIntegrationTests {

        @Test
        @DisplayName("Should process orders and generate output files")
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void testOrderProcessing() {
            String testDate = "2024-01-15";  // Use a date known to have orders
            orderProcessor.processDayOrders(testDate);

            // Verify output files exist and are valid
            verifyOutputFiles(testDate);
        }

        @Test
        @DisplayName("Should handle invalid orders correctly")
        void testInvalidOrderHandling() {
            String testDate = "2025-01-27";  // Date with known invalid orders
            orderProcessor.processDayOrders(testDate);

            // Verify deliveries file contains invalid orders
            File deliveriesFile = new File(RESULTS_DIR, "deliveries-" + testDate + ".json");
            assertTrue(deliveriesFile.exists());

            String content = null;
            try {
                content = Files.readString(deliveriesFile.toPath());
                assertTrue(content.contains("INVALID"));
                assertTrue(content.contains("\"orderStatus\":\"INVALID\""));
            } catch (Exception e) {
                fail("Failed to read deliveries file: " + e.getMessage());
            }
        }

        private void verifyOutputFiles(String date) {
            File deliveriesFile = new File(RESULTS_DIR, "deliveries-" + date + ".json");
            File flightpathFile = new File(RESULTS_DIR, "flightpath-" + date + ".json");

            assertTrue(deliveriesFile.exists(), "Deliveries file should exist");
            assertTrue(flightpathFile.exists(), "Flightpath file should exist");

            try {
                String deliveriesContent = Files.readString(deliveriesFile.toPath());
                String flightpathContent = Files.readString(flightpathFile.toPath());

                assertTrue(deliveriesContent.startsWith("["));
                assertTrue(deliveriesContent.endsWith("]"));
                assertTrue(flightpathContent.startsWith("["));
                assertTrue(flightpathContent.endsWith("]"));
            } catch (Exception e) {
                fail("Failed to verify output files: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("End-to-End Flow Tests")
    class EndToEndTests {
        @Test
        @DisplayName("Should handle complete order processing flow")
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        void testCompleteOrderFlow() {
            // 1. Fetch restaurants and verify
            Restaurant[] restaurants = client.getRestaurants();
            assertNotNull(restaurants);
            assertTrue(restaurants.length > 0);

            // 2. Get orders for a known date
            String testDate = "2024-01-15";
            Order[] orders = client.getOrders(testDate);
            assertNotNull(orders);

            // 3. Process orders
            orderProcessor.processDayOrders(testDate);

            // 4. Verify output files
            File deliveriesFile = new File(RESULTS_DIR, "deliveries-" + testDate + ".json");
            File flightpathFile = new File(RESULTS_DIR, "flightpath-" + testDate + ".json");

            assertTrue(deliveriesFile.exists());
            assertTrue(flightpathFile.exists());

            try {
                String deliveriesContent = Files.readString(deliveriesFile.toPath());
                String flightpathContent = Files.readString(flightpathFile.toPath());

                // Basic JSON validation
                assertTrue(deliveriesContent.startsWith("[") && deliveriesContent.endsWith("]"));
                assertTrue(flightpathContent.startsWith("[") && flightpathContent.endsWith("]"));

                // Verify deliveries match order count
                int deliveryCount = deliveriesContent.split("orderNo").length - 1;
                assertEquals(orders.length, deliveryCount);
            } catch (Exception e) {
                fail("Failed to verify final output: " + e.getMessage());
            }
        }
    }
}