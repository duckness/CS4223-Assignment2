import java.lang.Math;
import java.util.Hashtable;

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
    private int update;
    private int busRead;
    private int privateData;
    private int sharedData;
    private int writeHitMemory;

    private boolean isStalled;
    private boolean smSendBusUpdate;
    private int smSendBusUpdateAddress;
    private BusOperation previousOtherOperation;

    public Cache (int cacheSize, int associativity, int blockSize, Protocol proto, int cacheCoreNumber) {
        cacheAccesses = 0;
        memoryAccesses = 0;
        readHit = 0;
        readMiss = 0;
        writeHit = 0;
        writeMiss = 0;
        update = 0;
        busRead = 0;
        privateData = 0;
        sharedData = 0;
        writeHitMemory = 0;

        isStalled = false;
        smSendBusUpdate = false;
        smSendBusUpdateAddress = 0;
        previousOtherOperation = new BusOperation(Transaction.NULL, -1, -1); // for first round of checks

        this.cacheSize = cacheSize;
        this.associativity = associativity;
        this.blockSize = blockSize;
        this.protocol = proto;
        this.cacheCoreNumber = cacheCoreNumber;

        arraySize = cacheSize/blockSize;                            // this is the number of "rows" in the cache

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
                checkSharedPrivate(row.get(i).state);
                readHit += 1;
                reorderCache(row, i, tag);
                return;
            }
        }
        // from here, cache missed.
        readMiss += 1;
        isStalled = true;
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) { // guranteed to be INVALID state
            Bus.putTransactionInBus(new BusOperation(Transaction.BUS_READ, cacheCoreNumber, address));
        } else { //DRAGON
            Bus.putTransactionInBus(new BusOperation(Transaction.PROCESSOR_READ_MISS, cacheCoreNumber, address));
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
                checkSharedPrivate(row.get(i).state);
                writeHit++;
                if ((protocol == Protocol.MSI || protocol == Protocol.MESI) && row.get(i).state == State.SHARED_CLEAN) {
                    Bus.putTransactionInBus(new BusOperation(Transaction.BUS_READ_EXCLUSIVE, cacheCoreNumber, address));
                } else if (protocol == Protocol.DRAGON) {
                    if (row.get(i).state == State.SHARED_CLEAN || row.get(i).state == State.SHARED_MODIFIED) {
                        Bus.putTransactionInBus(new BusOperation(Transaction.BUS_UPDATE, cacheCoreNumber, address));
                        // check if other cache has an update, use this var to change state to M if needed
                        Bus.isBusUpdateReceived = false;
                    } else if (row.get(i).state == State.EXCLUSIVE) { // special case
                        row.set(i, new CacheBlock(State.MODIFIED, tag));
                    }
                }
                reorderCache(row, i, tag);
                return;
            }
        }
        // from here, cache missed.
        writeMiss += 1;
        isStalled = true;
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) { // guranteed to be INVALID state
            Bus.putTransactionInBus(new BusOperation(Transaction.BUS_READ_EXCLUSIVE, cacheCoreNumber, address));
        } else { //DRAGON
            Bus.putTransactionInBus(new BusOperation(Transaction.PROCESSOR_WRITE_MISS, cacheCoreNumber, address));
        }
    }

    public void busSnoop (int cycles) {
        BusOperation operation = Bus.operation;
        // special case of get to Sm state from "invalid" state, need to send bus update to other cache
        if (smSendBusUpdate) {
            Bus.putTransactionInBus(new BusOperation(Transaction.BUS_UPDATE, cacheCoreNumber, smSendBusUpdateAddress));
            smSendBusUpdate = false;
        }
        if (operation == null) { // case where no operation
            return;
            // check if transaction is from current core and if transaction is completed
        } else if (cacheCoreNumber == Bus.operation.cacheCore && Bus.isTransactionCompleted) {
            if (operation.transaction == Transaction.BUS_FLUSH) {
                Bus.hasCacheReceivedTransaction = true;
                return;
            }
            if (Bus.hasTransactionResult) { // i.e, busRead/busReadX is successful
                busRead += 1;
                // case of no other cache in Sm/Sc state (busUpdate unsuccessful)
                if (!Bus.isBusUpdateReceived) {
                    operation.lastTransaction = Transaction.BUS_UPDATE;
                    Bus.isBusUpdateReceived = true;
                }
                updateSelfCache(operation);
                isStalled = false;
                Bus.hasCacheReceivedTransaction = true;
            } else {                        // i.e, busRead/busReadX is unsuccessful, read from main memory
                memoryAccesses += 1;
                if (operation.transaction == Transaction.BUS_READ_EXCLUSIVE || operation.transaction == Transaction.PROCESSOR_WRITE_MISS) {
                    writeHitMemory += 1;
                }
                isStalled = true;
                Bus.operation.lastTransaction = Transaction.BUS_READ;
                if (protocol == Protocol.DRAGON) {
                    Bus.operation.lastTransaction = operation.transaction; // WrMiss or RdMiss
                }
                Bus.memoryAccessExtraCycles(cycles);
            }
            // else this operation is from other cores, and we must update our state to reflect their operation
        } else {
            if (previousOtherOperation.equals(operation)) {
                return;
            }
            if (operation.transaction != Transaction.BUS_FLUSH) {
                updateFromOtherCache(operation);
            }
            previousOtherOperation = operation;
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
                    // uses RdMiss & WrMiss to indicate
                }
                break;
            case PROCESSOR_READ_MISS:
                state = State.SHARED_CLEAN;
                break;
            case PROCESSOR_WRITE_MISS:
                state = State.SHARED_MODIFIED;
                smSendBusUpdate = true;
                smSendBusUpdateAddress = operation.address;
                break;
            case BUS_READ_EXCLUSIVE: // MSI/MESI only
                state = State.MODIFIED;
                break;
            case BUS_UPDATE: // DRAGON only
                state = State.SHARED_MODIFIED;
                break;
            default:
                break;
        }
        if (protocol == Protocol.MESI && operation.lastTransaction == Transaction.BUS_READ) {
            state = State.EXCLUSIVE;
        }

        if (protocol == Protocol.DRAGON) {
            if (operation.lastTransaction == Transaction.PROCESSOR_READ_MISS) {
                state = State.EXCLUSIVE;
            } else if (operation.lastTransaction == Transaction.PROCESSOR_WRITE_MISS) {
                state = State.MODIFIED;
            } else if (operation.lastTransaction == Transaction.BUS_UPDATE) {
                state = State.MODIFIED;
            }
        }

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
                    update += 1;
                }
                Bus.flushToBus(cacheCoreNumber);
                break;
            case SHARED_CLEAN:
                if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                    update += 1;
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
                    update += 1;
                }
                Bus.flushToBus(cacheCoreNumber);
                break;
            case EXCLUSIVE:
                if (transaction == Transaction.BUS_READ) {
                    block.state = State.SHARED_CLEAN;
                } else if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                    update += 1;
                }
                Bus.sendDataToBus();
                break;
            case SHARED_CLEAN:
                if (transaction == Transaction.BUS_READ_EXCLUSIVE) {
                    block.state = State.INVALID;
                    update += 1;
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
                if (transaction == Transaction.BUS_READ || transaction == Transaction.PROCESSOR_WRITE_MISS) {
                    block.state = State.SHARED_MODIFIED;
                }
                Bus.sendDataToBus();
                Bus.flushToBus(cacheCoreNumber);
                break;
            case SHARED_MODIFIED:
                if (transaction == Transaction.BUS_UPDATE || transaction == Transaction.PROCESSOR_WRITE_MISS) {
                    block.state = State.SHARED_CLEAN;
                    //Bus Update here (don't need to do anything to data)
                    Bus.isBusUpdateReceived = true;
                    update += 1;
                } else if (transaction == Transaction.BUS_READ) {
                    Bus.flushToBus(cacheCoreNumber);
                }
                Bus.sendDataToBus();
                break;
            case SHARED_CLEAN:
                if (transaction == Transaction.BUS_UPDATE || transaction == Transaction.PROCESSOR_READ_MISS) {
                    //Bus Update here (don't need to do anything to data)
                    Bus.isBusUpdateReceived = true;
                    update += 1;
                }
                Bus.sendDataToBus();
                break;
            case EXCLUSIVE:
                if (transaction == Transaction.BUS_READ || transaction == Transaction.PROCESSOR_READ_MISS) {
                    block.state = State.SHARED_CLEAN;
                }
                Bus.sendDataToBus();
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
     * the actual data we need for the assignment
     */
    public Hashtable<String, Integer> retrieveCacheResults() {
        System.out.println("Data Cache miss rate(total miss/total cache access attempts): " + ((double)memoryAccesses / (double)cacheAccesses));
        Hashtable<String, Integer> results = new Hashtable<>();

        // amount of data traffic busread/buswrite/busupdate
        int traffic;
        if (protocol == Protocol.MSI || protocol == Protocol.MESI) {
            traffic = (busRead - memoryAccesses) * blockSize;
        } else { // dragon
            // bus update receives 1 word = 4 bytes
            // we count number of received bytes
            traffic = update * 4;
        }
        results.put("traffic", traffic);
        // number of invalidations/updates
        results.put("update", update);
        // distribution of private/shared data (own cache only)
        results.put("private", privateData);
        results.put("shared", sharedData);
        // average write latency
        results.put("hitSelf", writeHit);
        results.put("hitOther", writeMiss - writeHitMemory);
        results.put("hitMemory", writeHitMemory);

        return results;
    }

    /**
     * Check if cache is waiting for some operation on bus/main memory to complete
     * @return true/false if above
     */
    public boolean isCacheStalled () {
        return isStalled;
    }

    private void checkSharedPrivate (State state) {
        if (state == State.MODIFIED || state == State.EXCLUSIVE) {
            privateData += 1;
        } else {// state SHARED_CLEAN SHARED_MODIFIED
            sharedData += 1;
        }
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
