


// ======= Imports =======
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.RestServiceClient;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RestServiceClientTest {
    private static final String VALID_BASE_URL = "https://ilp-rest-2024.azurewebsites.net";

    @Test
    void constructor_WithValidUrl_Success() {
        assertDoesNotThrow(() -> new RestServiceClient(VALID_BASE_URL));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "http://ilp-rest-2024.azurewebsites.net",
            "https://other-domain.com",
            "https://ilp-rest-2024.azurewebsites.com",
            "ftp://ilp-rest-2024.azurewebsites.net"
    })
    void constructor_WithInvalidUrl_ThrowsException(String invalidUrl) {
        assertThrows(IllegalArgumentException.class,
                () -> new RestServiceClient(invalidUrl));
    }

    @Test
    void getRestaurants_Success() {
        RestServiceClient client = new RestServiceClient(VALID_BASE_URL);
        assertDoesNotThrow(() -> {
            Restaurant[] restaurants = client.getRestaurants();
            assertNotNull(restaurants);
            assertTrue(restaurants.length > 0);
        });
    }

    @Test
    void getOrders_WithValidDate_Success() {
        RestServiceClient client = new RestServiceClient(VALID_BASE_URL);
        assertDoesNotThrow(() -> {
            Order[] orders = client.getOrders("2025-01-23");
            assertNotNull(orders);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "2025/01/23",
            "25-01-2023",
            "2025-13-01",
            "2025-01-32"
    })
    void getOrders_WithInvalidDate_ThrowsException(String invalidDate) {
        RestServiceClient client = new RestServiceClient(VALID_BASE_URL);
        assertThrows(IllegalArgumentException.class,
                () -> client.getOrders(invalidDate));
    }

    @Test
    void getCentralArea_Success() {
        RestServiceClient client = new RestServiceClient(VALID_BASE_URL);
        assertDoesNotThrow(() -> {
            NamedRegion centralArea = client.getCentralArea();
            assertNotNull(centralArea);
            assertNotNull(centralArea.name());
            assertTrue(centralArea.vertices().length > 0);
        });
    }

    @Test
    void getNoFlyZones_Success() {
        RestServiceClient client = new RestServiceClient(VALID_BASE_URL);
        assertDoesNotThrow(() -> {
            NamedRegion[] noFlyZones = client.getNoFlyZones();
            assertNotNull(noFlyZones);
            assertTrue(noFlyZones.length > 0);
        });
    }


}