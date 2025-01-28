package uk.ac.ed.inf.ilp.constant;

/**
 * the status an order can have
 */
public enum OrderStatus {

    /**
     * it is invalid
     */
    INVALID,

    /**
     * the state is valid
     */
    VALID,

    /**
     * the state is currently undefined
     */
    UNDEFINED,
    DELIVERED,
    VALID_BUT_NOT_DELIVERED
}
