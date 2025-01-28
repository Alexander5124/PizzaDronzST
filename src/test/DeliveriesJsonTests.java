import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import uk.ac.ed.inf.DeliveriesJson;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import java.util.ArrayList;
import java.util.List;


class DeliveriesJsonTest {
    private List<DeliveriesJson> deliveries;
    private Order validOrder;
    private Order invalidOrder;

    @BeforeEach
    void setUp() {
        deliveries = new ArrayList<>();
        validOrder = createTestOrder("TEST123", OrderStatus.VALID_BUT_NOT_DELIVERED,
                OrderValidationCode.NO_ERROR, 2500);
        invalidOrder = createTestOrder("TEST456", OrderStatus.INVALID,
                OrderValidationCode.CARD_NUMBER_INVALID, 0);
    }

    private Order createTestOrder(String orderNo, OrderStatus status,
                                  OrderValidationCode validationCode, int price) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setOrderStatus(status);
        order.setOrderValidationCode(validationCode);
        order.setPriceTotalInPence(price);
        return order;
    }

    @Test
    void testValidDeliveryUpdate() {
        DeliveriesJson.updateDeliveryInfo(deliveries, validOrder);

        assertEquals(1, deliveries.size());
        DeliveriesJson delivery = deliveries.get(0);
        assertEquals("TEST123", delivery.orderNo());
        assertEquals("VALID_BUT_NOT_DELIVERED", delivery.orderStatus());
        assertEquals("NO_ERROR", delivery.orderValidationCode());
        assertEquals(2500, delivery.costInPence());
    }

    @Test
    void testInvalidDeliveryUpdate() {
        DeliveriesJson.updateDeliveryInfo(deliveries, invalidOrder);

        assertEquals(1, deliveries.size());
        DeliveriesJson delivery = deliveries.get(0);
        assertEquals("TEST456", delivery.orderNo());
        assertEquals("INVALID", delivery.orderStatus());
        assertEquals("CARD_NUMBER_INVALID", delivery.orderValidationCode());
        assertEquals(0, delivery.costInPence());
    }

    @Test
    void testAllValidationCodes() {
        for (OrderValidationCode code : OrderValidationCode.values()) {
            Order order = createTestOrder("TEST-" + code.name(),
                    OrderStatus.INVALID, code, 1000);
            DeliveriesJson.updateDeliveryInfo(deliveries, order);
        }

        assertEquals(OrderValidationCode.values().length, deliveries.size());
        for (int i = 0; i < deliveries.size(); i++) {
            assertEquals(OrderValidationCode.values()[i].name(),
                    deliveries.get(i).orderValidationCode());
        }
    }

    @Test
    void testMultipleDeliveryUpdates() {
        Order validOrderWithPizzaError = createTestOrder("TEST789",
                OrderStatus.INVALID, OrderValidationCode.PIZZA_NOT_DEFINED, 0);

        DeliveriesJson.updateDeliveryInfo(deliveries, validOrder);
        DeliveriesJson.updateDeliveryInfo(deliveries, invalidOrder);
        DeliveriesJson.updateDeliveryInfo(deliveries, validOrderWithPizzaError);

        assertEquals(3, deliveries.size());
        assertEquals("NO_ERROR", deliveries.get(0).orderValidationCode());
        assertEquals("CARD_NUMBER_INVALID", deliveries.get(1).orderValidationCode());
        assertEquals("PIZZA_NOT_DEFINED", deliveries.get(2).orderValidationCode());
    }

    @Test
    void testNullOrderHandling() {
        DeliveriesJson.updateDeliveryInfo(deliveries, null);
        assertTrue(deliveries.isEmpty());
    }

    @Test
    void testNullFieldsHandling() {
        Order nullOrder = new Order();
        nullOrder.setOrderNo(null);
        nullOrder.setOrderStatus(null);
        nullOrder.setOrderValidationCode(OrderValidationCode.UNDEFINED);
        nullOrder.setPriceTotalInPence(0);

        DeliveriesJson.updateDeliveryInfo(deliveries, nullOrder);

        assertEquals(1, deliveries.size());
        DeliveriesJson delivery = deliveries.get(0);
        assertNull(delivery.orderNo());
        assertEquals("null", delivery.orderStatus());
        assertEquals("UNDEFINED", delivery.orderValidationCode());
        assertEquals(0, delivery.costInPence());
    }
    @Test
    void testDeliveryRecordCreation() {
        DeliveriesJson delivery = new DeliveriesJson("TEST789",
                OrderStatus.VALID_BUT_NOT_DELIVERED.name(),
                OrderValidationCode.RESTAURANT_CLOSED.name(), 3000);

        assertEquals("TEST789", delivery.orderNo());
        assertEquals("VALID_BUT_NOT_DELIVERED", delivery.orderStatus());
        assertEquals("RESTAURANT_CLOSED", delivery.orderValidationCode());
        assertEquals(3000, delivery.costInPence());
    }
}