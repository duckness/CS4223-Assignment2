/**
 * Encode transactions.
 */
public enum Transaction {
    PROCESSOR_READ,
    PROCESSOR_READ_MISS,
    PROCESSOR_WRITE,
    PROCESSOR_WRITE_MISS,
    BUS_READ,
    BUS_READ_EXCLUSIVE,
    BUD_WRITE_BACK,
    BUS_UPDATE;
}
