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

    private int arraySize;
    private int offsetBits;
    private int indexBits;
    private int tagBits;

    private int cacheAccesses;
    private int readHit;
    private int readMiss;
    private int writeHit;
    private int writeMiss;

    public Cache (int cacheSize, int associativity, int blockSize, Protocol proto) {
        cacheAccesses = 0;
        readHit = 0;
        readMiss = 0;
        writeHit = 0;
        writeMiss = 0;

        this.cacheSize = cacheSize;
        this.associativity = associativity;
        this.blockSize = blockSize;
        this.protocol = proto;

        arraySize = cacheSize/blockSize/associativity;              // this is the number of "rows" in the cache

        l1cache = new FixedList<>(arraySize);                       // create a fixed size ArrayList as our cache
        for (int i = 0; i < arraySize; i++) {
            l1cache.set(i, new FixedQueue<>(associativity));        // create fixed size queue for associativity (LRU)
        }

        offsetBits = binaryLog(blockSize);
        indexBits = binaryLog(cacheSize/blockSize);
        tagBits = 32 - offsetBits - indexBits;

        System.out.println("Protocol: " + this.protocol);
        System.out.println("Cache Size: " + this.cacheSize + " Bytes");
        System.out.println("Associativity: " + this.associativity);
        System.out.println("Block Size: " + this.blockSize + " Bytes");
        System.out.println("Offset: " + this.offsetBits + " Bits");
        System.out.println("Set Index: " + this.indexBits + " Bits");
        System.out.println("Tag bits: " + this.tagBits + " Bits");
    }

    public void readCache(int address) {
        cacheAccesses += 1;

        int index = getIndex(address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(address);

        for (int i = 0; i < associativity; i++) {
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
        // TODO: something on bus for readMiss
    }

    public void writeCache(int address) {
        cacheAccesses += 1;

        int index = getIndex(address);
        FixedQueue<CacheBlock> row = l1cache.get(index);
        int tag = getTag(address);

        for (int i = 0; i < associativity; i++) {
            if (tag == row.get(i).tag) {
                // take care of the case of MSI/MESI and invalid state
                if ((protocol == Protocol.MSI || protocol == Protocol.MESI) && row.get(i).state == State.INVALID) {
                    break;
                }
                writeHit++;
                // TODO: something on bus for writeHit
                reorderCache(row, i, tag);
                return;
            }
        }
        // from here, cache missed.
        writeMiss += 1;
        // TODO: something on bus for writeMiss
    }

    /**
     * @param number
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
        row.add(new CacheBlock(currentState,tag));
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
