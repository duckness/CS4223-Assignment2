import java.lang.Math;

/**
 * The L1 cache.
 */

public class Cache {
    /**
     * Static variables:
     * Words are 32 bits = 4 bytes (minimum blockSize/associativity must be 4)
     *
     * Inputs (stdin):
     * “cacheSize”: cache size in bytes (4096)
     * “associativity”: associativity of the cache (1)
     * “blockSize”: block size in bytes (16)
     *
     * Inputs (trace):
     * "instruction": LDR or STR or NOP
     * "address" <- 32bits
     *
     * Intermediates:
     * "offset"bits = log-base-2 (blockSize) bits
     * "index" or arraySize = cacheSize/blockSize
     * "index"bits = log-base-2 ("index") bits
     * "tag" = (32bits - "offset"bits - "index"bits) bits
     *
     *  address
     *
     *
     *
     * Mapping main memory to cache:
     * Cache Index = (Block Number) modulo (Number of Cache Blocks)
     *
     * Cache structure:
     * "valid" variable
     * "tag" variable = address/Block number/Number of Cache Blocks
     *
     * Not needed:
     * "data" variable, we don't need/have data.
     */

    private FixedList<FixedQueue<CacheBlock>> l1cache;

    private int cacheSize;
    private int associativity;
    private int blockSize;
    private Protocol protocol;
    private int cacheCoreNumber; // the core which the cache is for

    private int arraySize;
    private int offsetBits;
    private int indexBits;
    private int tagBits;

    private int cacheAccesses;
    private int memoryAccesses;
    private int readHit;
    private int readMiss;
    private int writeHit;
    private int writeMiss;
    private int currentCycle;

    private boolean isStalled;
    private BusOperation previousOperation;

    public Cache (int cacheSize, int associativity, int blockSize, Protocol proto, int cacheCoreNumber) {
        cacheAccesses = 0;
        memoryAccesses = 0;
        readHit = 0;
        readMiss = 0;
        writeHit = 0;
        writeMiss = 0;
        currentCycle = 0;

        isStalled = false;
        previousOperation = null;

        this.cacheSize = cacheSize;
        this.associativity = associativity;
        this.blockSize = blockSize;
        this.protocol = proto;
        this.cacheCoreNumber = cacheCoreNumber;

        arraySize = cacheSize/blockSize/associativity;              // this is the number of "rows" in the cache

        l1cache = new FixedList<>(arraySize);                       // create a fixed size ArrayList as our cache
        for (int i = 0; i < arraySize; i++) {
            l1cache.set(i, new FixedQueue<>(associativity));        // create fixed size queue for associativity (LRU)
        }

        offsetBits = binaryLog(blockSize);
        indexBits = binaryLog(cacheSize/blockSize);
        tagBits = 32 - offsetBits - indexBits;

        System.out.println("Protocol: " + this.protocol);
        System.out.println("Core: " + this.cacheCoreNumber);
        System.out.println("Cache Size: " + this.cacheSize + " Bytes");
        System.out.println("Associativity: " + this.associativity);
        System.out.println("Block Size: " + this.blockSize + " Bytes");
        System.out.println("Offset: " + this.offsetBits + " Bits");
        System.out.println("Set Index: " + this.indexBits + " Bits");
        System.out.println("Tag bits: " + this.tagBits + " Bits");
        System.out.println();
    }

    public void readCache (int address) {
        cacheAccesses += 1;

        int index = getIndex(address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(address);

        for (int i = 0; i < row.size(); i++) {
            if (tag == row.get(i).tag) {
                // take care of the case of MSI/MESI and invalid state
                if ((protocol == Protocol.MSI || protocol == Protocol.MESI) && row.get(i).state == State.INVALID) {
                    break;
                }
                readHit += 1;
                reorderCache(row, i, tag);
                return;
            }
        }
        // from here, cache missed.
        readMiss += 1;
        isStalled = true;
        // TODO: something on bus for readMiss
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) { // guranteed to be INVALID state
            // bus function new BusOperation(Transaction.BUS_READ, cacheCoreNumber, address);
            // change to SHARED_CLEAN state once data received
        } else { //DRAGON
            // TODO
            //bus function new BusOperation(Transaction.BUS_READ_MISS, cacheCoreNumber, address);
        }

    }

    public void writeCache (int address) {
        cacheAccesses += 1;

        int index = getIndex(address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(address);

        for (int i = 0; i < row.size(); i++) {
            if (tag == row.get(i).tag) {
                // take care of the case of MSI/MESI and invalid state
                if ((protocol == Protocol.MSI || protocol == Protocol.MESI) && row.get(i).state == State.INVALID) {
                    break;
                }
                writeHit++;
                isStalled = true;
                // TODO: something on bus for writeHit
                if ((protocol == Protocol.MSI || protocol == Protocol.MESI) && row.get(i).state == State.SHARED_CLEAN) {
                    //bus function new BusOperation(Transaction.BUS_READ_EXCLUSIVE, cacheCoreNumber, address);
                } else if (protocol == Protocol.DRAGON) {
                    // TODO
                    //do something for dragon Dx
                }
                reorderCache(row, i, tag);
                return;
            }
        }
        // from here, cache missed.
        writeMiss += 1;
        isStalled = true;
        // TODO: something on bus for writeMiss
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) { // guranteed to be INVALID state
        } else { //DRAGON
            // TODO
        }
    }

    public void busSnoop (int cycles) {
        currentCycle = cycles;
        // TODO: get operation from bus
        // TODO: write bus class
        if (operation = null) { // case where no operation
            return;
            // check if transaction is from current core and if transaction is completed
        } else if (cacheCoreNumber == Bus.getCore() && Bus.isTransactionCompleted) {
            if (operation.transaction == Transaction.BUS_FLUSH) {
                // Bus.setReceived();
                return;
            }
            if (Bus.hasTransactionResult) { //i.e, busRead/busReadX is successful
                updateSelfCache(operation);
                isStalled = false;
                // Bus.setReceived();
            } else {                        // i.e, busRead/busReadX is unsuccessful, read from main memory
                memoryAccesses += 1;
                isStalled = true;
                // TODO: main memory access
            }
            // else this operation is from other cores, and we must update our state to reflect their operation
        } else {
            if (previousOperation.equals(operation)) {
                return;
            }
            if (operation.transaction != Transaction.BUS_FLUSH) {
                updateFromOtherCache(operation);
            }
            previousOperation = operation;
        }
    }

    /**
     * This method is called when there is an operation from the bus (from this core) that we need to execute that updates the cache
     * @param operation
     */
    private void updateSelfCache (BusOperation operation) {
        int index = getIndex(operation.address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(operation.address);
        State state = null;
        switch (operation.transaction) {
            case BUS_READ:
                if (protocol == Protocol.MSI || protocol == Protocol.MESI) {
                    state = State.SHARED_CLEAN;
                } else { //DRAGON
                    // TODO
                }
                break;
            case BUS_READ_EXCLUSIVE:
                if (protocol == Protocol.MSI || protocol == Protocol.MESI) {
                    state = State.SHARED_MODIFIED;
                } else { //DRAGON
                    // TODO
                }
                break;
            case BUS_UPDATE: // DRAGON only
                // TODO
                break;
            default:
                break;
        }
        if (operation.lastTransaction == Transaction.BUS_READ && protocol == Protocol.MESI) {
            state = State.EXCLUSIVE;
        }
        // TODO: DRAGON lastTransaction

        if (state == null) {
            System.out.println("updateSelfCache error in assigning state");
            return;
        }

        for (int i = 0; i < associativity; i++) {
            // case where the CacheSet queue is not full and no existing row to be updated (need to break loop early)
            if (i == row.size() && i < associativity) {
                row.add(new CacheBlock(state, tag));
                break;
            // case where CacheSet has an existing row to be updated
            } else if (tag == row.get(i).tag) {
                row.set(i, new CacheBlock(state, tag));
                reorderCache(row, i, tag);
                break;
            // case where CacheSet queue is full and no existing row to be updated
            } else {
                row.add(new CacheBlock(state, tag));  // as it is a fixed size queue, will auto remove the LRU
            }
        }
    }
    /**
     * This method is called when there is an operation from the bus (from OTHER core) that we need to execute that updates
     * the cache. In this scenario, the block must exist in the cache before we can update.
     * @param operation
     */
    private void updateFromOtherCache (BusOperation operation) {
        int index = getIndex(operation.address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(operation.address);

        for (int i = 0; i < row.size(); i++) {
            if (tag == row.get(i).tag) {
                CacheBlock block = row.get(i);
                switch (protocol) {
                    case MSI:
                        msiProtocolBus(block, operation.transaction);
                        break;
                    case MESI:
                        mesiProtocolBus(block, operation.transaction);
                        break;
                    case DRAGON:
                        dragonProtocolBus(block, operation.transaction);
                        break;
                    default:
                        break;
                }
                row.set(i, block);
                reorderCache(row, i, tag);
                break;
            }
        }
    }

    private void msiProtocolBus (CacheBlock block, Transaction transaction) {
        switch (block.state) {
            case MODIFIED:
                if (transaction == Transaction.BUS_READ) {
                    block.state = State.SHARED_CLEAN;
                } else if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                }
                // TODO: flush data on bus
                break;
            case SHARED_CLEAN:
                if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                }
                break;
            case INVALID:
                break;
            default:
                System.out.println("ERROR ERROR PARAMETER");
                break;
        }
    }

    private void mesiProtocolBus (CacheBlock block, Transaction transaction) {
        switch (block.state) {
            case MODIFIED:
                if (transaction == Transaction.BUS_READ) {
                    block.state = State.SHARED_CLEAN;
                } else if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                }
                // TODO: flush data on bus
                break;
            case EXCLUSIVE:
                if (transaction == Transaction.BUS_READ) {
                    block.state = State.SHARED_CLEAN;
                } else if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                }
                // TODO: flush data on bus
                break;
            case SHARED_CLEAN:
                if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                }
                break;
            case INVALID:
                break;
            default:
                System.out.println("ERROR ERROR PARAMETER");
                break;

        }
    }

    private void dragonProtocolBus (CacheBlock block, Transaction transaction) {
        switch (block.state) {
            case MODIFIED:
                if (transaction == Transaction.BUS_READ) {
                    block.state = State.SHARED_MODIFIED;
                }
                // TODO: flush data on bus
                break;
            case EXCLUSIVE:
                if (transaction == transaction.BUS_READ) {
                    block.state = State.SHARED_CLEAN;
                }
                break;
            case SHARED_CLEAN:
                break;
            case SHARED_MODIFIED:
                if (transaction == transaction.BUS_UPDATE) {
                    block.state = State.SHARED_CLEAN;
                }
                // TODO: bus update???
                break;
            default:
                System.out.println("ERROR ERROR PARAMETER");
                break;
        }
    }

    /**
     * quick and dirty way to get current stats
     */
    public void printCacheStats () {
        System.out.println("Number of times the cache was accessed: " + cacheAccesses);
        System.out.println("Number of times main memory was accessed: " + memoryAccesses);
        System.out.println("Number of times there was a read hit: " + readHit);
        System.out.println("Number of times there was a read miss: " + readMiss);
        System.out.println("Number of times there was a write hit: " + writeHit);
        System.out.println("Number of times there was a write miss: " + writeMiss);
    }

    /**
     * Check if cache is waiting for some operation on bus/main memory to complete
     * @return true/false if above
     */
    public boolean isCacheStalled () {
        return isStalled;
    }

    /**
     * @param number we are finding the the base 2 logarithm of this input
     * @return log base 2 of the input integer
     */
    private static int binaryLog (int number) {
        return (int)Math.round(Math.log((double)number) / Math.log((double)2));
    }

    /**
     * A cache hit, due to temporal locality, that cache needs to be reordered to the front of the queue.
     * Reorder the queue by simply removing and adding to it.
     * @param row the queue to be reordered
     * @param index the item to be reordered
     * @param tag the value of the item to be reordered
     */
    private void reorderCache (FixedQueue<CacheBlock> row, int index, int tag) {
        State currentState = row.get(index).state;
        row.remove(index);
        row.add(new CacheBlock(currentState, tag));
    }

    /**
     * This method will get the `index` of the FixedList that we need to see see
     * done by bitshifting left to get rid of the front bits, then bitshifting right to get rid of the rest
     * @param address is the number to be truncated
     * @return the `index`
     */
    private int getIndex (int address) {
        return (address << tagBits) >>> (tagBits + offsetBits);
    }

    /**
     * This method will help us get the `tag` value
     * done by bitshifting right until only `tagBits` number of bits are left
     * @param address is the number to be truncated
     * @return the `tag`
     */
    private int getTag (int address) {
        return address >>> (32 - tagBits);
    }
}
