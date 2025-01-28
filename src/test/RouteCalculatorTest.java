import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.ac.ed.inf.RestServiceClient;
import uk.ac.ed.inf.RouteCalculator;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import uk.ac.ed.inf.LngLatHandler;
import java.util.stream.*;

class RouteCalculatorTest {
    @Mock
    private RestServiceClient mockClient;
    private RouteCalculator calculator;
    private static final String TEST_URL = "https://ilp-rest-2024.azurewebsites.net";
    private static final double TOLERANCE = 0.0001;
    private static final LngLat AT_POSITION = new LngLat(-3.186874, 55.944494);
    private static final double[] VALID_ANGLES = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
            180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupMockRegions();
        calculator = new RouteCalculator(TEST_URL);
    }

    private void setupMockRegions() {
        // Mock central area
        LngLat[] centralVertices = {
                new LngLat(-3.192473, 55.942617),
                new LngLat(-3.192473, 55.946233),
                new LngLat(-3.184319, 55.946233),
                new LngLat(-3.184319, 55.942617)
        };
        NamedRegion central = new NamedRegion("central", centralVertices);

        // Mock no-fly zones
        LngLat[] nfz1Vertices = {
                new LngLat(-3.192473, 55.944425),
                new LngLat(-3.192473, 55.946425),
                new LngLat(-3.194473, 55.946425),
                new LngLat(-3.194473, 55.944425)
        };
        NamedRegion nfz1 = new NamedRegion("NFZ1", nfz1Vertices);

        when(mockClient.getCentralArea()).thenReturn(central);
        when(mockClient.getNoFlyZones()).thenReturn(new NamedRegion[]{nfz1});
    }

    @Nested
    @DisplayName("Basic Path Finding Tests")
    class BasicPathFindingTests {
        @Test
        @DisplayName("Path exists between valid points")
        void findValidPath() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.188873, 55.944728);
            List<LngLat> path = calculator.findPath(start, end, false);

            assertNotNull(path);
            assertFalse(path.isEmpty());
            assertEquals(start, path.get(0));
            assertTrue(new LngLatHandler().isCloseTo(path.get(path.size()-1), end));
        }

        @Test
        @DisplayName("Path respects minimum distance constraints")
        void testMinimumDistance() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.188873, 55.944728);
            List<LngLat> path = calculator.findPath(start, end, false);

            for (int i = 0; i < path.size() - 1; i++) {
                double distance = new LngLatHandler().distanceTo(path.get(i), path.get(i + 1));
                assertTrue(distance <= 0.00015, "Move distance exceeds maximum");
            }
        }

        @Test
        @DisplayName("Path follows compass angles")
        void testCompassAngles() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.188873, 55.944728);
            List<LngLat> path = calculator.findPath(start, end, false);

            for (int i = 0; i < path.size() - 1; i++) {
                double angle = calculateAngle(path.get(i), path.get(i + 1));
                boolean isValidAngle = false;
                for (double validAngle : VALID_ANGLES) {
                    if (Math.abs(angle - validAngle) < TOLERANCE ||
                            Math.abs(angle - validAngle - 360) < TOLERANCE) {
                        isValidAngle = true;
                        break;
                    }
                }
                assertTrue(isValidAngle, "Invalid angle: " + angle);
            }
        }
    }

    @Nested
    @DisplayName("No-Fly Zone Tests")
    class NoFlyZoneTests {
        @Test
        @DisplayName("Path avoids no-fly zones")
        void testNoFlyZoneAvoidance() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.195473, 55.945425);
            List<LngLat> path = calculator.findPath(start, end, false);

            assertFalse(path.isEmpty());
            for (LngLat point : path) {
                assertFalse(isInNoFlyZone(point),
                        "Point " + point + " is in no-fly zone");
            }
        }

        @Test
        @DisplayName("Path segments don't intersect no-fly zones")
        void testNoIntersection() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.195473, 55.945425);
            List<LngLat> path = calculator.findPath(start, end, false);

            for (int i = 0; i < path.size() - 1; i++) {
                assertFalse(intersectsNoFlyZone(path.get(i), path.get(i + 1)),
                        String.format("Path segment from %s to %s intersects no-fly zone",
                                path.get(i), path.get(i + 1)));
            }
        }
    }

    @Nested
    @DisplayName("Central Area Tests")
    class CentralAreaTests {
        @Test
        @DisplayName("Return path stays in central area once entered")
        void testCentralAreaConstraint() {
            LngLat start = new LngLat(-3.188873, 55.944728);
            LngLat end = AT_POSITION;
            List<LngLat> path = calculator.findPath(start, end, true);

            boolean inCentral = false;
            for (LngLat point : path) {
                if (isInCentralArea(point)) {
                    inCentral = true;
                }
                if (inCentral) {
                    assertTrue(isInCentralArea(point),
                            "Point " + point + " left central area after entering");
                }
            }
        }
    }

    @Nested
    @DisplayName("Cache and Performance Tests")
    class CacheAndPerformanceTests {
        @Test
        @DisplayName("Path caching works correctly")
        void testCaching() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.188873, 55.944728);

            List<LngLat> path1 = calculator.findPath(start, end, false);
            List<LngLat> path2 = calculator.findPath(start, end, false);

            assertIterableEquals(path1, path2);
        }

        @Test
        @DisplayName("Calculate path within time limit")
        void testPerformance() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.195473, 55.945425);

            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                List<LngLat> path = calculator.findPath(start, end, false);
                assertFalse(path.isEmpty());
            });
        }

        @Test
        @DisplayName("Reset cache clears stored paths")
        void testCacheReset() {
            LngLat start = AT_POSITION;
            LngLat end = new LngLat(-3.188873, 55.944728);

            List<LngLat> path1 = calculator.findPath(start, end, false);
            calculator.resetState();
            List<LngLat> path2 = calculator.findPath(start, end, false);

            // Paths should be equal but not same instances
            assertNotSame(path1, path2);
            assertEquals(path1.size(), path2.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        @Test
        @DisplayName("Handle same start and end points")
        void testSamePoints() {
            List<LngLat> path = calculator.findPath(AT_POSITION, AT_POSITION, false);
            assertNotNull(path);
            assertEquals(1, path.size());
            assertEquals(AT_POSITION, path.get(0));
        }

        @Test
        @DisplayName("Handle points near no-fly zone boundary")
        void testNearNoFlyZone() {
            LngLat nearNoFly = new LngLat(-3.192474, 55.944426);
            List<LngLat> path = calculator.findPath(AT_POSITION, nearNoFly, false);
            assertFalse(path.isEmpty());
            assertFalse(isInNoFlyZone(path.get(path.size()-1)));
        }

        @Test
        @DisplayName("Handle unreachable end point")
        void testUnreachablePoint() {
            // Point completely surrounded by no-fly zones
            LngLat unreachable = new LngLat(-3.193473, 55.945425);
            List<LngLat> path = calculator.findPath(AT_POSITION, unreachable, false);
            assertTrue(path.isEmpty());
        }
    }

    // Helper methods
    private double calculateAngle(LngLat a, LngLat b) {
        double angle = Math.toDegrees(Math.atan2(
                b.lat() - a.lat(),
                b.lng() - a.lng()
        ));
        return angle < 0 ? angle + 360 : angle;
    }

    private boolean isInNoFlyZone(LngLat point) {
        return Arrays.stream(mockClient.getNoFlyZones())
                .anyMatch(zone -> new LngLatHandler().isInRegion(point, zone));
    }

    private boolean intersectsNoFlyZone(LngLat a, LngLat b) {
        return Arrays.stream(mockClient.getNoFlyZones())
                .anyMatch(zone -> new LngLatHandler().doLineSegmentsIntersect(a, b, zone));
    }

    private boolean isInCentralArea(LngLat point) {
        return new LngLatHandler().isInCentralArea(point, mockClient.getCentralArea());
    }
}