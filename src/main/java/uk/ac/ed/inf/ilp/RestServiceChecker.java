package uk.ac.ed.inf.ilp;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.ilp.data.Order;

public class RestServiceChecker {
    public static void main(String[] args) {
        try {
            String baseUrl = "https://ilp-rest-2024.azurewebsites.net";
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/orders"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Raw response:");
            System.out.println(response.body());

            Order[] orders = mapper.readValue(response.body(), Order[].class);

            System.out.println("\nParsed orders:");
            for (Order order : orders) {
                System.out.println("\nOrder number: " + order.getOrderNo());
                System.out.println("Date: " + order.getOrderDate());
                System.out.println("Price: " + order.getPriceTotalInPence());
                System.out.println("Status: " + order.getOrderStatus());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}