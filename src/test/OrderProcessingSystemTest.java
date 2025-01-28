import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import uk.ac.ed.inf.OrderProcessingHandler;


public class OrderProcessingSystemTest {

    private OrderProcessingHandler orderProcessingHandler;

    @BeforeEach
    void setUp() {
        String realBaseUrl = "ilp-rest-2024.azurewebsites.net";
        orderProcessingHandler = new OrderProcessingHandler(realBaseUrl);
    }

    @Test
    @DisplayName("System Test (Real URL): processDayOrders => end-to-end with actual REST data")
    void testProcessDayOrdersReal() throws IOException {

        String dateStr = "2025-01-27";

        orderProcessingHandler.processDayOrders(dateStr);


        File deliveriesFile  = new File("resultfiles/deliveries-" + dateStr + ".json");
        File flightpathFile  = new File("resultfiles/flightpath-" + dateStr + ".json");
        File geojsonFile     = new File("resultfiles/drone-"      + dateStr + ".geojson");

        assertTrue(deliveriesFile.exists(),
                "deliveries-" + dateStr + ".json should be created if there were any orders on that date");
        assertTrue(flightpathFile.exists(),
                "flightpath-" + dateStr + ".json should be created if a route was calculated");
        assertTrue(geojsonFile.exists(),
                "drone-" + dateStr + ".geojson should be created for flight path visualization");


        String deliveriesContent = Files.readString(deliveriesFile.toPath());
        assertTrue(deliveriesContent.contains("orderNo") || deliveriesContent.contains("priceTotalInPence"),
                "Deliveries JSON should contain some order-related fields");


    }
}