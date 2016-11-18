import java.util.LinkedList;

/**
 * Stuff to do on the bus (~the wheels on the bus go round and round~).
 */
public class Bus {

    private static final int CYCLES_MEMORY = 100;
    private static final int CYCLES_WORD = 1; // use this for DRAGON?
    private static int cycles_block; // = wordsInBlock * CYCLES_WORD, use this for MSI/MESI

    public static boolean isTransactionCompleted;
    public static boolean hasTransactionResult;
    public static boolean hasCacheReceivedTransaction;
    public static boolean isBusUpdateReceived;
    public static BusOperation operation;
    public static int expectedCompletedCycle;
    private static LinkedList<BusOperation> allBusOperations;
    private static Protocol protocol;

    // static to make sure that only one copy exists
    public static void initBus(Protocol proto, int wordsPerBlock) {
        isTransactionCompleted = true;
        hasTransactionResult = false;
        hasCacheReceivedTransaction = true;
        isBusUpdateReceived = true;
        operation = null;
        protocol = proto;
        cycles_block = CYCLES_WORD * wordsPerBlock;
        /*
         * don't use Java Queue interface as it adds restrictions we don't want, need to be careful with using LinkedList
         * Add to front of LinkedList (unless it's flush, add to END) and remove from end of LinkedList
         */
        allBusOperations = new LinkedList<>();
        expectedCompletedCycle = -1;
    }

    /**
     * Add to the FRONT of the LinkedList!
     * @param operation
     */
    public static void putTransactionInBus(BusOperation operation) {
        allBusOperations.addFirst(operation);
    }

    /**
     * something that sounds better depending on context
     */
    public static void sendDataToBus() {
        hasTransactionResult = true;
    }

    /**
     * Not accessing main memory so don't care what is flushed
     * flush goes first though, so we need to add to the BACK of the linked list
     */
    public static void flushToBus(int cacheCore) {
        BusOperation busFlush = new BusOperation(Transaction.BUS_FLUSH, 0, cacheCore);
        allBusOperations.addLast(busFlush);
        hasTransactionResult = true;
    }

    public static void runBusTransactions(int currentCycle) {
        if (currentCycle >= expectedCompletedCycle) {
            isTransactionCompleted = true;
        } else {
            isTransactionCompleted = false;
        }

        if(hasCacheReceivedTransaction && !allBusOperations.isEmpty()) {
            hasTransactionResult = false;
            hasCacheReceivedTransaction = true;
            operation = allBusOperations.pollLast(); // get and remove last item in the LinkedList
            addAdditionalCycles(operation.transaction, currentCycle);
        }
    }

    public static void memoryAccessExtraCycles(int currentCycle) {
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) {
            expectedCompletedCycle = currentCycle + CYCLES_MEMORY - cycles_block;
        } else { // DRAGON
            expectedCompletedCycle = currentCycle + CYCLES_MEMORY - CYCLES_WORD;
        }
        accessMemory();
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
            if (transaction == Transaction.BUS_FLUSH) {
                expectedCompletedCycle = cycle + CYCLES_MEMORY - 1;
                accessMemory();
            } else {
                expectedCompletedCycle = cycle + cycles_block - 1; // we do this first, then if need memory access add additional cycles later
            }
        } else { // DRAGON
            // as DRAGON simply sends a WORD (1 cycle), we don't need to do anything as they will receive the item the next cycle
        }
    }
}
