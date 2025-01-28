import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import uk.ac.ed.inf.OrderProcessingHandler;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderProcessingSystemTimeTest {

    private OrderProcessingHandler orderProcessingHandler;

    @BeforeEach
    void setUp() {

        String baseUrl = "ilp-rest-2024.azurewebsites.net";
        orderProcessingHandler = new OrderProcessingHandler(baseUrl);
    }

    /**
     * System Test: processDayOrders, ensuring it finishes < 60 seconds.
     * The @Timeout(60) annotation means this entire test method fails if it runs longer than 60s.
     */
    @Test
    @DisplayName("System Test: processDayOrders should complete under 60s")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testProcessDayOrdersUnder60Seconds() throws IOException {

        String dateStr = "2025-01-27";
        orderProcessingHandler.processDayOrders(dateStr);

        File deliveriesFile  = new File("resultfiles/deliveries-" + dateStr + ".json");
        File flightpathFile  = new File("resultfiles/flightpath-" + dateStr + ".json");
        File geojsonFile     = new File("resultfiles/drone-"      + dateStr + ".geojson");

        if (deliveriesFile.exists()) {
            String deliveriesContent = Files.readString(deliveriesFile.toPath());
            // e.g. do some checks if you want 
            assertTrue(deliveriesContent.length() >= 2,
                    "deliveries JSON should not be trivially empty if orders exist");
        }
        if (flightpathFile.exists()) {
            String flightContent = Files.readString(flightpathFile.toPath());
            // e.g. do some checks if you want
            assertTrue(flightContent.length() >= 2,
                    "deliveries JSON should not be trivially empty if orders exist");
        }

        if (geojsonFile.exists()) {
            String geojsonContent= Files.readString(flightpathFile.toPath());
            // e.g. do some checks if you want
            assertTrue(geojsonContent.length() >= 2,
                    "deliveries JSON should not be trivially empty if orders exist");
        }
    }
}