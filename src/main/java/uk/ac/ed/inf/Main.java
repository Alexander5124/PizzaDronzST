package uk.ac.ed.inf;
//Main which initiates the system
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args){
        if (!areArgumentsValid(args)) {
            System.err.println("Please enter the Date and the correct REST service URL in that order");
            return;
    }

        String orderDate = args[0];
        String baseUrl = args[1];

        try {
            OrderProcessingHandler manager = new OrderProcessingHandler(baseUrl);
            manager.processDayOrders(orderDate);
        }
        catch (Exception e) {
            System.err.println("An error occurred during order process creation: " + e.getMessage());
        }
}
// method to ensure that there are the correct number of arguments inputted
private static boolean areArgumentsValid(String[] args) {
    return args.length == 2;
}
}