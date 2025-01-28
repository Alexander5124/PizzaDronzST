package uk.ac.ed.inf;



import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.util.ArrayList;
import java.util.List;

/**
 * class that performs all the processing for the orders
 * consisting of using all the other classes, such as routeCalculator and Rest service etc
 */
public class OrderProcessingHandler {
    private final RestServiceClient client;
    private final OrderValidator orderValidator;
    private final RouteCalculator routeCalculator;

    public OrderProcessingHandler(String baseURL) {
        this.client = new RestServiceClient(baseURL);
        this.routeCalculator = new RouteCalculator(baseURL);
        this.orderValidator = new OrderValidator();
    }

    public void processDayOrders(String orderDate) {
        try {
            System.out.println("Processing orders for date: " + orderDate);

            Order[] orders = client.getOrders(orderDate);
            Restaurant[] restaurants = client.getRestaurants();

            // Initialize collections even if there are no orders
            List<LngLat> flightPaths = new ArrayList<>();
            List<MovesJson> moves = new ArrayList<>();
            List<DeliveriesJson> deliveries = new ArrayList<>();

            if (orders.length == 0) {
                System.out.println("No orders returned from REST for date: " + orderDate);
            } else {
                LngLat startPoint = new LngLat(-3.186874, 55.944494);
                processEachOrderAndGenerateDeliveryInfo(orders, restaurants,
                        startPoint, flightPaths, moves, deliveries);
            }

            // Always save results, even if empty
            JsonUtils.saveResults(orderDate, moves, deliveries, flightPaths);

        } catch (Exception e) {
            System.err.println("Error processing orders for the day: " + e.getMessage());
            throw e; // Rethrow to ensure test failure
        }
    }

    private void processEachOrderAndGenerateDeliveryInfo(
            Order[] orders,
            Restaurant[] restaurants,
            LngLat startPoint,
            List<LngLat> flightPaths,
            List<MovesJson> moves,
            List<DeliveriesJson> deliveries
    ) {
        int iteration = 0;
        for (Order order : orders) {
            iteration++;
            processSingleOrder(order, restaurants, startPoint, flightPaths, moves, iteration);
            DeliveriesJson.updateDeliveryInfo(deliveries, order);
            System.out.println("Processed order index: " + iteration);
        }
    }

    private void processSingleOrder(
            Order order,
            Restaurant[] restaurants,
            LngLat startPoint,
            List<LngLat> flightPaths,
            List<MovesJson> moves,
            int numOfOrderProcessed
    ) {
        try {
            Order processedOrder = orderValidator.validateOrder(order, restaurants);

            if (processedOrder.getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                System.out.println("Processing order " + numOfOrderProcessed);
                handleValidOrder(processedOrder, restaurants, startPoint, flightPaths, moves);
                order.setOrderStatus(OrderStatus.DELIVERED);
            } else {
                System.out.println("Order " + numOfOrderProcessed + " is invalid: "
                        + processedOrder.getOrderValidationCode());
                order.setOrderStatus(OrderStatus.INVALID);
            }
        } catch (Exception e) {
            System.err.println("Error processing order " + numOfOrderProcessed + ": " + e.getMessage());
            order.setOrderStatus(OrderStatus.INVALID);
        }
    }

    private void handleValidOrder(
            Order order,
            Restaurant[] restaurants,
            LngLat startPoint,
            List<LngLat> flightPaths,
            List<MovesJson> moves
    ) {
        try {
            Restaurant destination = OrderValidator.restaurantFinder(order, restaurants);
            if (destination == null) {
                throw new IllegalStateException("Restaurant not found for order: " + order.getOrderNo());
            }

            System.out.println("Calculating path to " + destination.location());

            // Calculate path to restaurant
            List<LngLat> pathToDestination = routeCalculator.findPath(startPoint, destination.location(), false);
            if (!pathToDestination.isEmpty()) {
                flightPaths.addAll(pathToDestination);
                MovesJson.addMoves(moves, pathToDestination, order.getOrderNo());
                MovesJson.addHoverMove(moves, destination.location(), order.getOrderNo());
            }

            // Calculate return path
            List<LngLat> returnPath = routeCalculator.findPath(destination.location(), startPoint, true);
            if (!returnPath.isEmpty()) {
                flightPaths.addAll(returnPath);
                MovesJson.addMoves(moves, returnPath, order.getOrderNo());
                if (!moves.isEmpty()) {  // Only add hover move if there are previous moves
                    MovesJson.addHoverMove(moves, startPoint, order.getOrderNo());
                }
            }

        } catch (Exception e) {
            System.err.println("Error in handling valid order: " + e.getMessage());
            throw e; // Rethrow to ensure proper error handling
        }
    }
}