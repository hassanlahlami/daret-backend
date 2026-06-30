package ma.daret.payments.port;

/** Direction of a money movement through the payment partner. */
public enum PaymentDirection {
    /** Pull a contribution from a member. */
    COLLECT,
    /** Send the pot to a cycle's recipient. */
    DISBURSE
}
