package uk.ac.ed.inf.ilp;

import uk.ac.ed.inf.RestServiceClient;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.Order;
public class QuickTest {
    public static void main(String[] args) {
        RestServiceClient client = new RestServiceClient("https://ilp-rest-2024.azurewebsites.net/");

        Restaurant[] restaurants = client.getRestaurants();
        System.out.println("\nRestaurants:");
        for (Restaurant restaurant : restaurants) {
            System.out.printf("- %s at (%.6f, %.6f)%n",
                    restaurant.name(), restaurant.location().lng(), restaurant.location().lat());
        }

        NamedRegion[] noFlyZones = client.getNoFlyZones();
        System.out.println("\nNo-fly zones:");
        for (NamedRegion zone : noFlyZones) {
            System.out.println("- " + zone.name());
        }
    }
}