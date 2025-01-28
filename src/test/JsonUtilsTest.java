import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.JsonUtils;
import uk.ac.ed.inf.MovesJson;
import uk.ac.ed.inf.DeliveriesJson;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {
    private static ObjectMapper objectMapper;
    @TempDir
    Path tempDir;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void saveJsonToFile_ValidData_CreatesFile() throws Exception {
        String filePath = tempDir.resolve("test.json").toString();
        String jsonData = "{\"test\": \"data\"}";

        JsonUtils.saveJsonToFile(filePath, jsonData);

        File savedFile = new File(filePath);
        assertTrue(savedFile.exists());
        JsonNode savedNode = objectMapper.readTree(savedFile);
        JsonNode expectedNode = objectMapper.readTree(jsonData);
        assertEquals(expectedNode, savedNode);
    }

    @Test
    void saveJsonToFile_NonexistentDirectory_CreatesDirectoryAndFile() throws Exception {
        String filePath = tempDir.resolve("subdir/test.json").toString();
        String jsonData = "{\"test\": \"data\"}";

        JsonUtils.saveJsonToFile(filePath, jsonData);

        File savedFile = new File(filePath);
        assertTrue(savedFile.exists());
        assertTrue(savedFile.getParentFile().exists());
    }

    @Test
    void saveJsonToFile_InvalidPath_HandlesError() {
        String invalidPath = "\0invalid/path/test.json";
        String jsonData = "{\"test\": \"data\"}";

        assertDoesNotThrow(() -> JsonUtils.saveJsonToFile(invalidPath, jsonData));
    }

    @Test
    void convertToGeoJson_ValidCoordinates_ReturnsValidGeoJson() throws Exception {
        List<LngLat> coordinates = Arrays.asList(
                new LngLat(-0.1275, 51.507222),
                new LngLat(-0.1280, 51.507500)
        );

        String geoJson = JsonUtils.convertToGeoJson(coordinates);
        JsonNode jsonNode = objectMapper.readTree(geoJson);

        assertEquals("Feature", jsonNode.get("type").asText());
        assertEquals("LineString", jsonNode.get("geometry").get("type").asText());
        assertTrue(jsonNode.get("geometry").get("coordinates").isArray());
        assertEquals(2, jsonNode.get("geometry").get("coordinates").size());
    }

    @Test
    void convertToGeoJson_EmptyList_ReturnsValidGeoJsonWithEmptyCoordinates() throws Exception {
        List<LngLat> coordinates = List.of();

        String geoJson = JsonUtils.convertToGeoJson(coordinates);
        JsonNode jsonNode = objectMapper.readTree(geoJson);

        assertEquals(0, jsonNode.get("geometry").get("coordinates").size());
    }

    @Test
    void saveGeoJsonToFile_ValidData_SavesPrettified() throws Exception {
        String filePath = tempDir.resolve("test.geojson").toString();
        String geoJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[1,1],[2,2]]}}";

        JsonUtils.saveGeoJsonToFile(filePath, geoJson);

        File savedFile = new File(filePath);
        assertTrue(savedFile.exists());
        String content = new String(Files.readAllBytes(savedFile.toPath()));
        assertTrue(content.contains("\n")); // Verify pretty printing
    }

    @Test
    void saveResults_ValidData_CreatesAllFiles() throws Exception {
        String orderDate = "2024-01-22";
        List<MovesJson> moves = List.of(new MovesJson("ORDER123", -3.186874, 55.944494, 45.0, -3.186974, 55.944594));
        List<DeliveriesJson> deliveries = List.of(new DeliveriesJson("ORDER123", "2024-01-22", "DELIVERED", 50));
        List<LngLat> flightPaths = List.of(new LngLat(-0.1275, 51.507222));

        JsonUtils.saveResults(orderDate, moves, deliveries, flightPaths);

        File deliveriesFile = new File("resultfiles/deliveries-" + orderDate + ".json");
        File flightpathFile = new File("resultfiles/flightpath-" + orderDate + ".json");
        File droneFile = new File("resultfiles/drone-" + orderDate + ".geojson");

        assertTrue(deliveriesFile.exists());
        assertTrue(flightpathFile.exists());
        assertTrue(droneFile.exists());

        // Cleanup
        deliveriesFile.delete();
        flightpathFile.delete();
        droneFile.delete();
        new File("resultfiles").delete();
    }

    @Test
    void saveResults_InvalidData_HandlesError() {
        String orderDate = "2024-01-22";
        List<MovesJson> moves = null;
        List<DeliveriesJson> deliveries = null;
        List<LngLat> flightPaths = null;

        assertDoesNotThrow(() -> JsonUtils.saveResults(orderDate, moves, deliveries, flightPaths));
    }}