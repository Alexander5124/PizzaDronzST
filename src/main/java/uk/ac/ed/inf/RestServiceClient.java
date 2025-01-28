package uk.ac.ed.inf;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.regex.Pattern;
import com.google.gson.*;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class RestServiceClient {
    private final String baseURL;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private static final String ALLOWED_DOMAIN = "ilp-rest-2024.azurewebsites.net";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");

    public RestServiceClient(String baseURL) {
        this.baseURL = validateBaseUrl(baseURL);
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String validateBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }

        String processedUrl = url.trim();
        if (processedUrl.startsWith("http://")) {
            throw new IllegalArgumentException("HTTP protocol not allowed");
        }

        try {
            URI parsedUrl;
            if (processedUrl.startsWith("https://")) {
                parsedUrl = URI.create(processedUrl);
            } else {
                parsedUrl = URI.create("https://" + processedUrl);
            }

            String host = parsedUrl.getHost().toLowerCase();
            if (!host.equalsIgnoreCase(ALLOWED_DOMAIN) && !host.equalsIgnoreCase("localhost")) {
                throw new IllegalArgumentException(
                        "Invalid domain. Only " + ALLOWED_DOMAIN + " or localhost is allowed, but got: " + host
                );
            }

            return host.equals("localhost") ?
                    parsedUrl.toString().replaceAll("/+$", "") :
                    String.format("https://%s", ALLOWED_DOMAIN);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }

    private void validateDateInput(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }
        if (!DATE_PATTERN.matcher(date.trim()).matches()) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }

    private <T> T performHttpRequest(String endpoint, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseURL + sanitizeEndpoint(endpoint)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(response.body(), responseType);
        } catch (Exception e) {
            throw new RuntimeException("Error performing request: " + e.getMessage(), e);
        }
    }

    private String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        String sanitized = endpoint.replaceAll("\\.\\.|/+", "/");
        return sanitized.replaceAll("[^a-zA-Z0-9_/.-]", "");
    }

    public Restaurant[] getRestaurants() {
        return performHttpRequest("/restaurants", Restaurant[].class);
    }

    public Order[] getOrders(String date) {
        validateDateInput(date);
        LocalDate targetDate = LocalDate.parse(date);
        Order[] allOrders = performHttpRequest("/orders", Order[].class);
        return Arrays.stream(allOrders)
                .filter(order -> targetDate.equals(order.getOrderDate()))
                .toArray(Order[]::new);
    }

    public NamedRegion getCentralArea() {
        return performHttpRequest("/centralArea", NamedRegion.class);
    }

    public NamedRegion[] getNoFlyZones() {
        return performHttpRequest("/noFlyZones", NamedRegion[].class);
    }
}