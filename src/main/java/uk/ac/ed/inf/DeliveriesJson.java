package uk.ac.ed.inf;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.ed.inf.ilp.data.Order;
import java.util.List;


/**
 A record to represent ,in the Json output file,each instance of a delivery that has been processed
 with each delivery json object having an order number, order status, order validation code and cost
 */
public record DeliveriesJson(
        @JsonProperty("orderNo") String orderNo,
        @JsonProperty("orderStatus") String orderStatus,
        @JsonProperty("orderValidationCode") String orderValidationCode,
        @JsonProperty("costInPence") int costInPence
) {
    /**
     * Method to update the deliveries array, which contains all the pre-existing deliveryJsons,
     * by appending the most recent orders delivery status
     * @param deliveries - pre-existing delivery Json record
     * @param order the order object
     */
    public static void updateDeliveryInfo(List<DeliveriesJson> deliveries, Order order) {
        try {
            deliveries.add(new DeliveriesJson(order.getOrderNo(), String.valueOf(order.getOrderStatus()),
                    String.valueOf(order.getOrderValidationCode()), order.getPriceTotalInPence()));
        }
        catch (Exception e){
            System.err.println("Error in updating delivery information");
        }
    }


}