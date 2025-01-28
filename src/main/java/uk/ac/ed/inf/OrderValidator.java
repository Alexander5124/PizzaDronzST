package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class OrderValidator implements OrderValidation {

    /**
     * Finds the restaurant for a given order by matching the first pizza
     * @param order - order to find restaurant for
     * @param restaurants - array of available restaurants
     * @return matching restaurant or null if not found
     */
    public static Restaurant restaurantFinder(Order order, Restaurant[] restaurants) {
        if (order == null || order.getPizzasInOrder() == null ||
                order.getPizzasInOrder().length == 0) {
            return null;
        }

        String firstPizzaName = order.getPizzasInOrder()[0].name();

        for (Restaurant restaurant : restaurants) {
            for (Pizza menuPizza : restaurant.menu()) {
                if (Objects.equals(menuPizza.name(), firstPizzaName)) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    public boolean is_card_invalid(Order orderToValidate) {
        String cardNumber = orderToValidate.getCreditCardInformation().getCreditCardNumber();
        if (cardNumber == null) return true;
        return !cardNumber.matches("\\d{16}");
    }

    public boolean is_cvv_invalid(Order orderToValidate) {
        String cvv = orderToValidate.getCreditCardInformation().getCvv();
        if (cvv == null) return true;
        return !cvv.matches("\\d{3}");
    }

    public boolean is_expiry_date_invalid(Order orderToValidate) {
        try {
            String expiryDate = orderToValidate.getCreditCardInformation().getCreditCardExpiry();
            if (expiryDate == null) return true;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth cardExpiry = YearMonth.parse(expiryDate, formatter);
            YearMonth orderDate = YearMonth.from(orderToValidate.getOrderDate());

            return orderDate.isAfter(cardExpiry);
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private Restaurant findRestaurantForPizza(Pizza pizza, Restaurant[] restaurants) {
        for (Restaurant restaurant : restaurants) {
            for (Pizza menuPizza : restaurant.menu()) {
                if (Objects.equals(menuPizza.name(), pizza.name())) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    public boolean is_any_pizza_undefined(Order order, Restaurant[] definedRestaurants) {
        for (Pizza orderedPizza : order.getPizzasInOrder()) {
            boolean pizzaFound = false;
            for (Restaurant restaurant : definedRestaurants) {
                for (Pizza menuPizza : restaurant.menu()) {
                    if (Objects.equals(menuPizza.name(), orderedPizza.name())) {
                        pizzaFound = true;
                        break;
                    }
                }
                if (pizzaFound) break;
            }
            if (!pizzaFound) return true;
        }
        return false;
    }

    public boolean is_max_pizza_count_exceeded(Order order) {
        return order.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER;
    }

    public boolean is_restaurant_closed(Order order, Restaurant[] definedRestaurants) {
        Restaurant restaurant = findRestaurantForPizza(order.getPizzasInOrder()[0], definedRestaurants);
        if (restaurant == null) return true;

        DayOfWeek orderDay = order.getOrderDate().getDayOfWeek();
        for (DayOfWeek openDay : restaurant.openingDays()) {
            if (openDay == orderDay) return false;
        }
        return true;
    }

    public boolean are_pizza_from_multiple_orders(Order order, Restaurant[] definedRestaurants) {
        Restaurant firstRestaurant = null;

        for (Pizza orderedPizza : order.getPizzasInOrder()) {
            Restaurant currentRestaurant = findRestaurantForPizza(orderedPizza, definedRestaurants);
            if (currentRestaurant == null) continue;

            if (firstRestaurant == null) {
                firstRestaurant = currentRestaurant;
            } else if (!firstRestaurant.equals(currentRestaurant)) {
                return true;
            }
        }
        return false;
    }

    public boolean is_total_cost_incorrect(Order order, Restaurant[] definedRestaurants) {
        int calculatedTotal = SystemConstants.ORDER_CHARGE_IN_PENCE;
        Restaurant restaurant = findRestaurantForPizza(order.getPizzasInOrder()[0], definedRestaurants);

        if (restaurant == null) return true;

        for (Pizza orderedPizza : order.getPizzasInOrder()) {
            boolean pizzaFound = false;
            for (Pizza menuPizza : restaurant.menu()) {
                if (Objects.equals(menuPizza.name(), orderedPizza.name())) {
                    calculatedTotal += menuPizza.priceInPence();
                    pizzaFound = true;
                    break;
                }
            }
            if (!pizzaFound) return true;
        }

        return calculatedTotal != order.getPriceTotalInPence();
    }

    public static Restaurant rest(Order order, Restaurant[] restaurants) {
        if (order == null || order.getPizzasInOrder() == null ||
                order.getPizzasInOrder().length == 0) {
            return null;
        }

        String firstPizzaName = order.getPizzasInOrder()[0].name();

        for (Restaurant restaurant : restaurants) {
            for (Pizza menuPizza : restaurant.menu()) {
                if (Objects.equals(menuPizza.name(), firstPizzaName)) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        orderToValidate.setOrderStatus(OrderStatus.INVALID);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);

        if (is_card_invalid(orderToValidate)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }

        if (is_cvv_invalid(orderToValidate)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }

        if (is_expiry_date_invalid(orderToValidate)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        if (is_max_pizza_count_exceeded(orderToValidate)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            return orderToValidate;
        }

        if (is_any_pizza_undefined(orderToValidate, definedRestaurants)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            return orderToValidate;
        }

        if (are_pizza_from_multiple_orders(orderToValidate, definedRestaurants)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
            return orderToValidate;
        }

        if (is_restaurant_closed(orderToValidate, definedRestaurants)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            return orderToValidate;
        }

        if (is_total_cost_incorrect(orderToValidate, definedRestaurants)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            return orderToValidate;
        }

        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        return orderToValidate;
    }
}