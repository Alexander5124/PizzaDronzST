import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Set;
import uk.ac.ed.inf.OrderValidator;

import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.ed.inf.OrderValidator;
import uk.ac.ed.inf.ilp.constant.OrderStatus;

public class OrderValidatorTests {

    private OrderValidator orderValidator;
    private Restaurant restaurantA;
    private Restaurant restaurantB;

    private Pizza pizzaMargherita;
    private Pizza pizzaPepperoni;
    private Pizza pizzaHawaiian;

    // Create an Order with specified card info, pizzas, date, and total cost
    private Order createOrder(String cardNumber, String cvv, String expiry,
                              Pizza[] pizzas, LocalDateTime orderDate, int totalCost) {
        CreditCardInformation cardInfo = new CreditCardInformation(cardNumber, cvv, expiry);
        Order order = new Order();
        order.setPizzasInOrder(pizzas);
        order.setCreditCardInformation(cardInfo);
        // Set the order date from LocalDateTime
        order.setOrderDate(LocalDate.from(orderDate));
        order.setPriceTotalInPence(totalCost);
        return order;
    }

    @BeforeEach
    void setUp() {
        orderValidator = new OrderValidator();

        // Sample pizzas
        pizzaMargherita = new Pizza("Margherita", 800);
        pizzaPepperoni = new Pizza("Pepperoni", 900);
        pizzaHawaiian = new Pizza("Hawaiian", 1000);

        // Example restaurants
        restaurantA = new Restaurant(
                "RestaurantA",
                new LngLat(51.5007, -0.1246),
                new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY},
                new Pizza[]{pizzaMargherita, pizzaPepperoni}
        );

        restaurantB = new Restaurant(
                "RestaurantB",
                new LngLat(55.0000, -1.0000),
                new DayOfWeek[]{DayOfWeek.THURSDAY, DayOfWeek.FRIDAY},
                new Pizza[]{pizzaHawaiian, pizzaPepperoni}
        );
    }


    @Nested
    @DisplayName("is_card_invalid Method Tests")
    class CardInvalidTests {

        @Test
        @DisplayName("Valid card number => false (NOT invalid)")
        void validCardNumber() {

            Order order = createOrder(
                    "1234567890123456",  // valid
                    "123",               // valid CVV
                    "12/25",            // future expiry
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertFalse(
                    orderValidator.is_card_invalid(order),
                    "A proper 16-digit numeric card should not be invalid."
            );
        }

        @Test
        @DisplayName("Card number not all digits => true (invalid)")
        void cardNumberNonDigits() {

            Order order = createOrder(
                    "1234abcd90123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_card_invalid(order),
                    "A card number containing letters should be invalid.");
        }

        @Test
        @DisplayName("Card number too short => true (invalid)")
        void cardNumberTooShort() {
            // Fewer than 16 digits
            Order order = createOrder(
                    "1234567",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_card_invalid(order),
                    "A card number with length < 16 should be invalid.");
        }

        @Test
        @DisplayName("Card number too long => true (invalid)")
        void cardNumberTooLong() {
            // More than 16 digits
            Order order = createOrder(
                    "12345678901234567890",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_card_invalid(order),
                    "A card number with length > 16 should be invalid.");
        }
    }


    @Nested
    @DisplayName("is_cvv_invalid Method Tests")
    class CvvInvalidTests {

        @Test
        @DisplayName("Valid CVV => false (NOT invalid)")
        void validCvv() {
            // Exactly 3 digits
            Order order = createOrder(
                    "1234567890123456", // valid 16-digit
                    "123",              // valid 3-digit CVV
                    "22/26",           // future expiry
                    new Pizza[]{pizzaPepperoni},
                    LocalDateTime.of(2025, 12, 7, 15, 0),
                    1800
            );
            assertFalse(orderValidator.is_cvv_invalid(order),
                    "A 3-digit numeric CVV should not be invalid.");
        }

        @Test
        @DisplayName("CVV with non-digit => true (invalid)")
        void cvvContainsLetter() {
            // 3 chars but includes a letter
            Order order = createOrder(
                    "1234567890123456",
                    "12a",
                    "12/25",
                    new Pizza[]{pizzaPepperoni},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_cvv_invalid(order),
                    "A CVV containing letters should be invalid.");
        }

        @Test
        @DisplayName("CVV too short => true (invalid)")
        void cvvTooShort() {
            // Only 2 digits
            Order order = createOrder(
                    "1234567890123456",
                    "12",
                    "12/25",
                    new Pizza[]{pizzaPepperoni},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_cvv_invalid(order),
                    "A CVV with length < 3 should be invalid.");
        }

        @Test
        @DisplayName("CVV too long => true (invalid)")
        void cvvTooLong() {
            // 4 digits
            Order order = createOrder(
                    "1234567890123456",
                    "1234",
                    "12/25",
                    new Pizza[]{pizzaPepperoni},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_cvv_invalid(order),
                    "A CVV with length > 3 should be invalid.");
        }
    }


    @Nested
    @DisplayName("is_expiry_date_invalid Method Tests")
    class ExpiryDateInvalidTests {

        @Test
        @DisplayName("Future date => false (NOT invalid)")
        void validExpiryFutureDate() {
            // Card expires 12/25 => order date is 2024-12 => not expired yet
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2024, 12, 1, 15, 0),
                    1800
            );
            // Not expired
            assertFalse(orderValidator.is_expiry_date_invalid(order),
                    "Future expiry date should not be invalid.");
        }

        @Test
        @DisplayName("Expiry date is same as order month => false (NOT invalid)")
        void expiryDateSameMonth() {
            // order date: 2025-12 => card: 12/25 => same month => still valid
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 12, 1, 15, 0),
                    1800
            );
            assertFalse(orderValidator.is_expiry_date_invalid(order),
                    "Expiry date same as order date month should not be invalid.");
        }

        @Test
        @DisplayName("Past date => true (invalid)")
        void expiryDatePast() {
            // order date: 2025-01 => card: 12/24 => now it's expired
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/24",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 1, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_expiry_date_invalid(order),
                    "An expiry date in the past should be invalid.");
        }

        @Test
        @DisplayName("Bad format => true (invalid)")
        void invalidFormat() {
            // Format "yyyy-MM" => not "MM/yy"
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "2025-12",
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2024, 12, 1, 15, 0),
                    1800
            );
            assertTrue(orderValidator.is_expiry_date_invalid(order),
                    "An expiry date not in MM/yy format should be invalid.");
        }
    }

    // --------------------------------------------------
    // 4. TESTING is_any_pizza_undefined
    // --------------------------------------------------
    @Nested
    @DisplayName("is_any_pizza_undefined Method Tests")
    class PizzaUndefinedTests {

        @Test
        @DisplayName("All pizzas defined => false (NOT undefined)")
        void allPizzasDefined() {
            // Margherita & Pepperoni both exist in restaurantA
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    LocalDateTime.now(),
                    1800
            );
            assertFalse(
                    orderValidator.is_any_pizza_undefined(order, new Restaurant[]{restaurantA, restaurantB}),
                    "All pizzas exist in restaurantA, so should not be undefined."
            );
        }

        @Test
        @DisplayName("At least one pizza not in any restaurant => true (undefined)")
        void somePizzasUndefined() {
            Pizza unknownPizza = new Pizza("UnknownPizza", 600);
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, unknownPizza},
                    LocalDateTime.now(),
                    1800
            );
            assertTrue(
                    orderValidator.is_any_pizza_undefined(order, new Restaurant[]{restaurantA, restaurantB}),
                    "One pizza does not exist in any restaurant’s menu => undefined => true."
            );
        }
    }


    @Nested
    @DisplayName("is_max_pizza_count_exceeded Method Tests")
    class MaxPizzaCountExceededTests {

        @Test
        @DisplayName("Order <= max => false (NOT exceeded)")
        void notExceeded() {
            // Suppose max is 5. We only order 2.
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    LocalDateTime.now(),
                    1800
            );
            assertFalse(orderValidator.is_max_pizza_count_exceeded(order),
                    "2 pizzas does not exceed the max if it's 5.");
        }

        @Test
        @DisplayName("Order > max => true (exceeded)")
        void exceeded() {
            // Suppose max is 5. We order 6.
            Pizza[] sixPizzas = {
                    pizzaMargherita, pizzaMargherita,
                    pizzaPepperoni, pizzaPepperoni,
                    pizzaHawaiian, pizzaHawaiian
            };
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    sixPizzas,
                    LocalDateTime.now(),
                    1800
            );
            assertTrue(orderValidator.is_max_pizza_count_exceeded(order),
                    "6 pizzas should exceed the maximum of 5.");
        }
    }


    @Nested
    @DisplayName("is_restaurant_closed Method Tests")
    class RestaurantClosedTests {

        @Test
        @DisplayName("Restaurant is open => false (NOT closed)")
        void restaurantIsOpen() {
            // restaurantA open Monday, Tuesday, Wednesday
            LocalDateTime mondayDate = LocalDateTime.of(2025, Month.JANUARY, 6, 12, 0);
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita}, // belongs to A
                    mondayDate,
                    1800
            );
            assertFalse(orderValidator.is_restaurant_closed(order, new Restaurant[]{restaurantA, restaurantB}),
                    "Restaurant A is open on Monday => not closed => false.");
        }

        @Test
        @DisplayName("Restaurant is closed => true (closed)")
        void restaurantIsClosed() {
            // restaurantA is closed on Thursday
            LocalDateTime thursdayDate = LocalDateTime.of(2025, Month.JANUARY, 9, 12, 0);
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita},
                    thursdayDate,
                    1800
            );
            assertTrue(orderValidator.is_restaurant_closed(order, new Restaurant[]{restaurantA, restaurantB}),
                    "Restaurant A is closed on Thursday => true.");
        }
    }


    @Nested
    @DisplayName("are_pizza_from_multiple_orders Method Tests")
    class PizzaFromMultipleRestaurantsTests {

        @Test
        @DisplayName("All pizzas from same restaurant => false (NOT multiple)")
        void sameRestaurant() {
            // Both margherita & pepperoni belong to restaurantA
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    LocalDateTime.now(),
                    1800
            );
            assertFalse(orderValidator.are_pizza_from_multiple_orders(order, new Restaurant[]{restaurantA, restaurantB}),
                    "All pizzas come from restaurantA => false.");
        }

        @Test
        @DisplayName("Pizzas from different restaurants => true (multiple)")
        void multipleRestaurants() {
            // margherita => A; hawaiian => B
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaHawaiian},
                    LocalDateTime.now(),
                    1800
            );
            assertTrue(orderValidator.are_pizza_from_multiple_orders(order, new Restaurant[]{restaurantA, restaurantB}),
                    "Margherita belongs to A, Hawaiian belongs to B => true.");
        }
    }


    @Nested
    @DisplayName("is_total_cost_incorrect Method Tests")
    class TotalCostIncorrectTests {

        @Test
        @DisplayName("Total cost matches => false (NOT incorrect)")
        void totalCostMatches() {
            // If ORDER_CHARGE_IN_PENCE=200, then margherita(800)+pepperoni(900)=1700 => +200 =>1900
            int correctTotal = pizzaMargherita.priceInPence() + pizzaPepperoni.priceInPence()
                    + SystemConstants.ORDER_CHARGE_IN_PENCE;

            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    LocalDateTime.now(),
                    correctTotal
            );
            assertFalse(orderValidator.is_total_cost_incorrect(order, new Restaurant[]{restaurantA, restaurantB}),
                    "Calculated cost matches => false (not incorrect).");
        }

        @Test
        @DisplayName("Total cost does not match => true (incorrect)")
        void totalCostMismatch() {

            int incorrectTotal = 999999;
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    LocalDateTime.now(),
                    incorrectTotal
            );
            assertTrue(orderValidator.is_total_cost_incorrect(order, new Restaurant[]{restaurantA, restaurantB}),
                    "Calculated cost doesn't match => should be true (incorrect).");
        }
    }


    @Nested
    @DisplayName("validateOrder Method Tests")
    class ValidateOrderTests {

        @Test
        @DisplayName("Valid order => NO_ERROR and VALID_BUT_NOT_DELIVERED")
        void validOrder() {
            // Everything correct:
            // - 16-digit card
            // - 3-digit CVV
            // - Future expiry
            // - Pizzas from same restaurant, no max exceed
            // - Restaurant open
            // - Correct total
            LocalDateTime mondayDate = LocalDateTime.of(2025, Month.JANUARY, 6, 12, 0); // Monday => A open
            int correctTotal = pizzaMargherita.priceInPence() + pizzaPepperoni.priceInPence()
                    + SystemConstants.ORDER_CHARGE_IN_PENCE;

            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita, pizzaPepperoni},
                    mondayDate,
                    correctTotal
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.NO_ERROR, validated.getOrderValidationCode(),
                    "Valid order => NO_ERROR code.");
            assertEquals(OrderStatus.VALID_BUT_NOT_DELIVERED, validated.getOrderStatus(),
                    "Valid order => VALID_BUT_NOT_DELIVERED status.");
        }

        @Test
        @DisplayName("Expired credit card => EXPIRY_DATE_INVALID")
        void expiredCreditCard() {
            // Make sure card number & CVV are valid so that expiry check is the first to fail
            LocalDateTime orderDate = LocalDateTime.of(2025, 1, 1, 12, 0); // Jan 2025
            // Expiry is 12/24 => Dec 2024 => expired
            Order order = createOrder(
                    "1234567890123456",  // valid
                    "123",               // valid
                    "12/24",            // expired
                    new Pizza[]{pizzaMargherita},
                    orderDate,
                    1000 // correct cost for 1 margherita + 200 = 1000 total
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.EXPIRY_DATE_INVALID, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Max pizza count exceeded => MAX_PIZZA_COUNT_EXCEEDED")
        void maxPizzaCountExceeded() {

            Pizza[] manyPizzas = {
                    pizzaMargherita, pizzaMargherita,
                    pizzaPepperoni,  pizzaPepperoni,
                    pizzaHawaiian,   pizzaPepperoni // 6 pizzas
            };
            // "12/26" => Dec 2026 => not expired if order is in 2025
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/26",
                    manyPizzas,
                    LocalDateTime.of(2025, 1, 5, 12, 0),
                    9999 // arbitrary total
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Invalid card number => CARD_NUMBER_INVALID")
        void invalidCardNumber() {
            // Make sure expiry & CVV are valid => only card # fails
            Order order = createOrder(
                    "1234",   // invalid (too short)
                    "123",
                    "12/25",  // future expiry
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 1, 5, 12, 0),
                    9999
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.CARD_NUMBER_INVALID, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Invalid CVV => CVV_INVALID")
        void invalidCVV() {
            // Make card #, expiry valid => only CVV fails
            Order order = createOrder(
                    "1234567890123456", // 16 digits
                    "12a",              // not all digits
                    "12/25",            // future
                    new Pizza[]{pizzaMargherita},
                    LocalDateTime.of(2025, 1, 5, 12, 0),
                    9999
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.CVV_INVALID, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Undefined pizza => PIZZA_NOT_DEFINED")
        void pizzaNotDefined() {
            // Make sure card #, CVV, expiry all valid => only pizza fails
            Pizza unknownPizza = new Pizza("UnknownPizza", 1200);
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{unknownPizza},
                    LocalDateTime.of(2025, 1, 5, 12, 0),
                    1200 // This total might match unknownPizza's price, but it's "not in any restaurant"
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.PIZZA_NOT_DEFINED, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Pizzas from multiple restaurants => PIZZA_FROM_MULTIPLE_RESTAURANTS")
        void pizzaFromMultipleRestaurants() {
            // Card #, CVV, expiry valid
            // Margherita => restaurantA, Hawaiian => restaurantB
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita, pizzaHawaiian},
                    LocalDateTime.of(2025, 1, 5, 12, 0),
                    10000
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Restaurant closed => RESTAURANT_CLOSED")
        void restaurantClosed() {
            // Make sure the card #, CVV, expiry are valid => only closed restaurant fails
            LocalDateTime thursday = LocalDateTime.of(2025, Month.JANUARY, 9, 12, 0); // Thursday
            // restaurantA is closed on Thu
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    thursday,
                    1000 // correct total for 1 margherita + 200 = 1000
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.RESTAURANT_CLOSED, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }

        @Test
        @DisplayName("Total cost incorrect => TOTAL_INCORRECT")
        void totalIncorrect() {
            // Card #, CVV, expiry, etc. all valid => only cost fails
            // margherita=800 + ORDER_CHARGE=200 => correct total=1000
            // We set 999 => mismatch
            LocalDateTime monday = LocalDateTime.of(2025, Month.JANUARY, 6, 12, 0); // Monday => open
            Order order = createOrder(
                    "1234567890123456",
                    "123",
                    "12/25",
                    new Pizza[]{pizzaMargherita},
                    monday,
                    999
            );

            Order validated = orderValidator.validateOrder(order, new Restaurant[]{restaurantA, restaurantB});
            assertEquals(OrderValidationCode.TOTAL_INCORRECT, validated.getOrderValidationCode());
            assertEquals(OrderStatus.INVALID, validated.getOrderStatus());
        }
    }
}

