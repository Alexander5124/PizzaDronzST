
import uk.ac.ed.inf.LngLatHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class LngLatPerformanceTest {
    private LngLatHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LngLatHandler();
    }

    @Nested
    @DisplayName("Performance and Edge Case Tests")
    class PerformanceAndEdgeCaseTests {

        @Test
        @DisplayName("Large polygon performance")
        void testLargePolygonPerformance() {
            // Create a large polygon with 1000 vertices
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

            // Test multiple points with timeout
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                for (int i = 0; i < 1000; i++) {
                    double x = -2 + Math.random() * 4; // Random x between -2 and 2
                    double y = -2 + Math.random() * 4; // Random y between -2 and 2
                    handler.isInRegion(new LngLat(x, y), region);
                }
            }, "Large polygon operations should complete within reasonable time");
        }
    }
}