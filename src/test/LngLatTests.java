
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import uk.ac.ed.inf.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LngLatTests {
    private LngLatHandler handler;
    private static final double DELTA = 1e-10;

    @BeforeEach
    void setUp() {
        handler = new LngLatHandler();
    }


    @ParameterizedTest(name = "Polygon with {0} sides")
    @MethodSource("regularPolygonProvider")
    @DisplayName("Regular polygon tests")
    void testRegularPolygon(int sides, LngLat[] vertices, List<LngLat> insidePoints, List<LngLat> outsidePoints) {
        NamedRegion region = new NamedRegion("regular", vertices);

        for (LngLat point : insidePoints) {
            assertTrue(handler.isInRegion(point, region),
                    String.format("Point (%f, %f) should be inside %d-sided polygon",
                            point.lng(), point.lat(), sides));
        }

        for (LngLat point : outsidePoints) {
            assertFalse(handler.isInRegion(point, region),
                    String.format("Point (%f, %f) should be outside %d-sided polygon",
                            point.lng(), point.lat(), sides));
        }
    }

    // Test data providers
    static Stream<Arguments> regularPolygonProvider() {
        return Stream.of(
                createRegularPolygonTestCase(3),  // Triangle
                createRegularPolygonTestCase(4),  // Square
                createRegularPolygonTestCase(5),  // Pentagon
                createRegularPolygonTestCase(6)   // Hexagon
        );
    }


    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Distance calculation should be accurate")
        void testDistanceCalculation() {
            LngLat point1 = new LngLat(0, 0);
            LngLat point2 = new LngLat(3, 4);
            assertEquals(5.0, handler.distanceTo(point1, point2), DELTA,
                    "Distance should match Pythagorean theorem");
        }

    }

    @Nested
    @DisplayName("Polygon Tests")
    class PolygonTests {

        @Test
        @DisplayName("Complex concave polygon containment")
        void testComplexConcavePolygon() {
            LngLat[] vertices = {
                    new LngLat(0, 0),
                    new LngLat(2, 0),
                    new LngLat(2, 2),
                    new LngLat(1, 1),
                    new LngLat(0, 2)
            };
            NamedRegion region = new NamedRegion("complex", vertices);

            assertTrue(handler.isInRegion(new LngLat(0.5, 0.5), region),
                    "Point in main body should be inside");
            assertFalse(handler.isInRegion(new LngLat(1, 1.5), region),
                    "Point in concave part should be outside");
            assertTrue(handler.isInRegion(new LngLat(1.5, 0.5), region),
                    "Point in other section should be inside");
        }


    }

    @Nested
    @DisplayName("Line Intersection Tests")
    class LineIntersectionTests {

        @Test
        @DisplayName("Line segment intersection edge cases")
        void testLineIntersectionEdgeCases() {
            LngLat[] vertices = {
                    new LngLat(0, 0),
                    new LngLat(1, 0),
                    new LngLat(1, 1),
                    new LngLat(0, 1)
            };
            NamedRegion region = new NamedRegion("test", vertices);

            // Test collinear segments
            LngLat p1 = new LngLat(-1, 0);
            LngLat p2 = new LngLat(2, 0);
            assertTrue(handler.doLineSegmentsIntersect(p1, p2, region),
                    "Collinear overlapping segments should intersect");

            // Test parallel segments
            LngLat p3 = new LngLat(2, 0);
            LngLat p4 = new LngLat(2, 1);
            assertFalse(handler.doLineSegmentsIntersect(p3, p4, region),
                    "Parallel non-intersecting segments should not intersect");

            // Test T-junction
            LngLat p5 = new LngLat(0.5, -1);
            LngLat p6 = new LngLat(0.5, 0);
            assertTrue(handler.doLineSegmentsIntersect(p5, p6, region),
                    "T-junction should be detected as intersection");
        }

        @Test
        @DisplayName("Multiple intersection points")
        void testMultipleIntersections() {
            LngLat[] vertices = {
                    new LngLat(0, 0),
                    new LngLat(2, 0),
                    new LngLat(2, 2),
                    new LngLat(0, 2)
            };
            NamedRegion region = new NamedRegion("test", vertices);

            LngLat p1 = new LngLat(-1, 1);
            LngLat p2 = new LngLat(3, 1);
            assertTrue(handler.doLineSegmentsIntersect(p1, p2, region),
                    "Line crossing region twice should be detected as intersection");
        }
    }

    @Nested
    @DisplayName("Movement and Angle Tests")
    class MovementTests {

        @ParameterizedTest
        @CsvSource({
                "0, 1, 0",    // East
                "90, 0, 1",   // North
                "180, -1, 0", // West
                "270, 0, -1"  // South
        })
        @DisplayName("Cardinal direction movements")
        void testCardinalMovements(double angle, double expectedDx, double expectedDy) {
            LngLat start = new LngLat(0, 0);
            LngLat result = handler.nextPosition(start, angle);

            double expectedDistance = 0.00015; // DRONE_MOVE_DISTANCE
            double actualDistance = handler.distanceTo(start, result);

            assertEquals(expectedDistance, actualDistance, DELTA,
                    "Movement distance should match DRONE_MOVE_DISTANCE");

            // Normalize the actual movement vector
            double dx = result.lng() / actualDistance;
            double dy = result.lat() / actualDistance;

            assertEquals(expectedDx, dx, DELTA, "X direction should match expected");
            assertEquals(expectedDy, dy, DELTA, "Y direction should match expected");
        }

        @Test
        @DisplayName("Angle calculation consistency")
        void testAngleConsistency() {
            LngLat center = new LngLat(0, 0);
            LngLat point = new LngLat(1, 1);

            double angle = LngLatHandler.calculateAngle(center, point);
            LngLat newPoint = handler.nextPosition(center, angle);

            double originalAngle = Math.atan2(point.lat(), point.lng());
            double newAngle = Math.atan2(newPoint.lat(), newPoint.lng());

            double angleDiff = Math.abs(originalAngle - newAngle) % (2 * Math.PI);
            assertTrue(angleDiff < 0.01, "Angles should be consistent");
        }
    }

    @Nested
    @DisplayName("Performance and Edge Case Tests")
    class PerformanceAndEdgeCaseTests {

        @Test
        @DisplayName("Large polygon performance")
        void testLargePolygonPerformance() {
            LngLat[] vertices = new LngLat[1000];
            double radius = 1.0;

            for (int i = 0; i < 1000; i++) {
                double angle = 2 * Math.PI * i / 1000;
                vertices[i] = new LngLat(
                        radius * Math.cos(angle),
                        radius * Math.sin(angle)
                );
            }

            NamedRegion region = new NamedRegion("large", vertices);

            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                for (int i = 0; i < 1000; i++) {
                    double x = -2 + Math.random() * 4;
                    double y = -2 + Math.random() * 4;
                    handler.isInRegion(new LngLat(x, y), region);
                }
            }, "Large polygon operations should complete within reasonable time");
        }

        @Test
        @DisplayName("Numerical stability tests")
        void testNumericalStability() {
            // Test with very small numbers
            LngLat[] smallVertices = {
                    new LngLat(0.0000001, 0.0000001),
                    new LngLat(0.0000002, 0.0000001),
                    new LngLat(0.0000002, 0.0000002),
                    new LngLat(0.0000001, 0.0000002)
            };
            NamedRegion smallRegion = new NamedRegion("small", smallVertices);

            LngLat smallPoint = new LngLat(0.00000015, 0.00000015);
            assertTrue(handler.isInRegion(smallPoint, smallRegion),
                    "Should handle very small numbers correctly");

            // Test with very large numbers
            LngLat[] largeVertices = {
                    new LngLat(1000000, 1000000),
                    new LngLat(1000001, 1000000),
                    new LngLat(1000001, 1000001),
                    new LngLat(1000000, 1000001)
            };
            NamedRegion largeRegion = new NamedRegion("large", largeVertices);

            LngLat largePoint = new LngLat(1000000.5, 1000000.5);
            assertTrue(handler.isInRegion(largePoint, largeRegion),
                    "Should handle very large numbers correctly");
        }
    }

    @Nested
    @DisplayName("Central Area Tests")
    class CentralAreaTests {

        @Test
        @DisplayName("Central area validation")
        void testCentralAreaValidation() {
            LngLat[] vertices = {
                    new LngLat(0, 0),
                    new LngLat(1, 0),
                    new LngLat(1, 1),
                    new LngLat(0, 1)
            };

            // Test with correct name
            NamedRegion centralRegion = new NamedRegion("central", vertices);
            LngLat point = new LngLat(0.5, 0.5);
            assertDoesNotThrow(() -> handler.isInCentralArea(point, centralRegion),
                    "Should not throw for correctly named central area");

            // Test with incorrect name
            NamedRegion nonCentralRegion = new NamedRegion("not_central", vertices);
            assertThrows(IllegalArgumentException.class,
                    () -> handler.isInCentralArea(point, nonCentralRegion),
                    "Should throw for incorrectly named region");

            // Test with null region
            assertThrows(IllegalArgumentException.class,
                    () -> handler.isInCentralArea(point, null),
                    "Should throw for null region");
        }
    }



    private static Arguments createRegularPolygonTestCase(int sides) {
        double radius = 1.0;
        LngLat[] vertices = new LngLat[sides];
        List<LngLat> insidePoints = new ArrayList<>();
        List<LngLat> outsidePoints = new ArrayList<>();

        // Create vertices
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            vertices[i] = new LngLat(
                    radius * Math.cos(angle),
                    radius * Math.sin(angle)
            );
        }

        // Inside points
        insidePoints.add(new LngLat(0, 0));  // Center
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            insidePoints.add(new LngLat(
                    0.5 * radius * Math.cos(angle),
                    0.5 * radius * Math.sin(angle)
            ));
        }

        // Outside points
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            outsidePoints.add(new LngLat(
                    1.5 * radius * Math.cos(angle),
                    1.5 * radius * Math.sin(angle)
            ));
        }

        return Arguments.of(sides, vertices, insidePoints, outsidePoints);
    }
}