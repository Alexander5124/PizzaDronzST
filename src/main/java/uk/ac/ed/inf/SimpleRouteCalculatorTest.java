package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.List;

public class SimpleRouteCalculatorTest {
    public static void main(String[] args) {
        // Create an instance of SimpleRouteCalculator with the no-fly zones
        RouteCalculator routeCalculator = new RouteCalculator("https://ilp-rest-2024.azurewebsites.net/");
        LngLat endPoint = new LngLat(-3.186874, 55.944494);
        LngLat startPoint = new LngLat(-3.1839, 55.9445);

        List<LngLat> path = routeCalculator.findPath(startPoint, endPoint, true);

        if (path.isEmpty()) {
            System.out.println("No path found");
        } else {
            System.out.println("Path found with " + path.size() + " points:");
            for (LngLat point : path) {
                System.out.printf("lng: %.6f, lat: %.6f%n", point.lng(), point.lat());
            }

            String geoJsonFlightPath = JsonUtils.convertToGeoJson(path);
            JsonUtils.saveGeoJsonToFile("resultfiles/drone-tester.geojson", geoJsonFlightPath);
        }

    }
}
