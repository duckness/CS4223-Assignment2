import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * Stuff to do on the bus (~the wheels on the bus go round and round~).
 */
public class Bus {

    public static boolean isTransactionCompleted;
    public static boolean hasTransactionResult;
    public static boolean hasCacheReceivedTransaction;
    public static BusOperation operation;
    public static int expectedCompletedCycle;
    private static LinkedList<List<BusOperation>> allBusOperations;
    private static List<BusOperation> cycleOps;
    private static Protocol protocol;

    // static to make sure that only one copy exists
    // TODO: call this ONCE in Main class!!!!!
    public static void initBus(Protocol proto) {
        // don't use Java Queue interface as it adds restrictions we don't want, need to be careful with using LinkedList
        // normally, add to front of LinkedList and remove from end of LinkedList
        hasTransactionResult = false;
        hasCacheReceivedTransaction = true;
        operation = null;
        protocol = proto;

        allBusOperations = new LinkedList<List<BusOperation>>();
        cycleOps = new ArrayList<BusOperation>();
        expectedCompletedCycle = -1;
    }

    public static void putTransactionInBus(BusOperation operation) {
        // TODO
    }

    /**
     * something that sounds better depending on context
     */
    public static void sendDataToBus() {
        hasTransactionResult = true;
    }

    /**
     * Not accessing main memory so don't care what is flushed
     * flush goes first though
     */
    public static void flushToBus(int cacheCore) {
        BusOperation busFlush = new BusOperation(Transaction.BUS_FLUSH, 0, cacheCore);
        List<BusOperation> flush = new ArrayList<BusOperation>();
        flush.add(busFlush);
        allBusOperations.addLast(flush);
        hasTransactionResult = true;
    }

    // TODO: ADD THIS TO MAIN LOOP IN MAIN CLASS!!!!!
    public static void runBusTransactions() {
        // TODO
        /*
        if(hasCacheReceivedTransaction && !allBusOperations.isEmpty()) {
            hasTransactionResult = false;
            hasCacheReceivedTransaction = true;
            BusOperation operation;


            addAdditionalCycles(
        }
        */
    }

    /**
     * doesn't really do anything since we are not really accessing Main Memory
     * we also call this earlier to make sure that the "memory" is seen down the line
     */
    private static void accessMemory() {
        hasTransactionResult = true;
    }

    private static void addAdditionalCycles(Transaction transaction, int cycle) {
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) {
            // TODO
        } else { // DRAGON
            // TODO
        }
    }

}
