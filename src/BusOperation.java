/**
 * Stuff to do on the bus (~the wheels on the bus go round and round~).
 */
public class BusOperation implements Comparable<BusOperation>{

    public Transaction transaction;
    public Transaction lastTransaction;
    public int address;
    public int cacheCore;

    public BusOperation(Transaction transaction,int cacheCore, int address) {
        this.transaction = transaction;
        this.address = address;
        this.cacheCore = cacheCore;
        this.lastTransaction = null;
    }

    public BusOperation(Transaction transaction, int cacheCore, int address, Transaction lastTransaction) {
        this.transaction = transaction;
        this.address = address;
        this.cacheCore = cacheCore;
        this.lastTransaction = lastTransaction;
    }

    @Override
    public int compareTo(BusOperation operation) {
        if (this.transaction == operation.transaction && this.cacheCore == operation.cacheCore)
            return 1;
        else
            return 0;
    }
}
