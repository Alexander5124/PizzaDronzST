package uk.ac.ed.inf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.ed.inf.ilp.data.LngLat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JsonUtils {
    // object mapper used to map json features
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Saves JSON data to a file.
     *
     * @param filePath The path of the file to save.
     * @param jsonData The JSON data as a string.
     */
    public static void saveJsonToFile(String filePath, String jsonData) {
        try {
            if (filePath == null || filePath.contains("\0")) {
                System.err.println("Invalid file path");
                return;
            }
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !parentDir.toFile().exists()) {
                parentDir.toFile().mkdirs();
            }
            Object json = objectMapper.readValue(jsonData, Object.class);
            objectMapper.writeValue(new File(filePath), json);
        } catch (IOException e) {
            System.err.println("Error saving JSON data to file: " + e.getMessage());
        }
    }

    /**
     * Converts a list of LngLat coordinates to GeoJSON format.
     *
     * @param lngLatList The list of LngLat coordinates.
     * @return GeoJSON representation as a string.
     */

    public static String convertToGeoJson(List<LngLat> lngLatList) {

        //mapping of GeoJson properties
        ObjectNode geoJsonFeature = objectMapper.createObjectNode();
        geoJsonFeature.put("type", "Feature");


        ObjectNode properties = objectMapper.createObjectNode();
        geoJsonFeature.set("properties", properties);

        ObjectNode geometry = objectMapper.createObjectNode();
        geometry.put("type", "LineString");
        ArrayNode coordinates = objectMapper.createArrayNode();

        // Add coordinates to the array
        for (LngLat point : lngLatList) {
            ArrayNode coord = objectMapper.createArrayNode();
            coord.add(point.lng());
            coord.add(point.lat());
            coordinates.add(coord);
        }

        // Add coordinates to the LineString geometry
        geometry.set("coordinates", coordinates);
        geoJsonFeature.set("geometry", geometry);

        return geoJsonFeature.toString();
    }

    /**
     *
     * @param fileName - name of the file that the GeoJson should be named as
     * @param geoJsonData - the string of geoJson data
     */
    public static void saveGeoJsonToFile(String fileName, String geoJsonData){
        try (FileWriter file = new FileWriter(fileName)){
            ObjectMapper objectMapper = new ObjectMapper();
            Object json = objectMapper.readValue(geoJsonData, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            file.write(prettyJson);
        }
        catch (IOException e){
            System.err.println("Error in saving the GeoJson file");
        }
    }

    /**
     *
     * @param orderDate - date of the order
     * @param moves - the list of moves
     * @param deliveries - pre-existing result of the deliveries Json array
     * @param flightPaths - all the flightpath to be saved
     */
    public static void saveResults(String orderDate, List<MovesJson> moves, List<DeliveriesJson> deliveries, List<LngLat> flightPaths) {
        if (orderDate == null || moves == null || deliveries == null || flightPaths == null) {
            System.err.println("Invalid input data");
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonMoves = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moves);
            String jsonDeliveries = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(deliveries);
            String geoJsonFlightPath = JsonUtils.convertToGeoJson(flightPaths);

            JsonUtils.saveJsonToFile("resultfiles/deliveries-" + orderDate + ".json", jsonDeliveries);
            JsonUtils.saveJsonToFile("resultfiles/flightpath-" + orderDate + ".json", jsonMoves);
            JsonUtils.saveGeoJsonToFile("resultfiles/drone-" + orderDate + ".geojson", geoJsonFlightPath);
        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        }
    }

}


