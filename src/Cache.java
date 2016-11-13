import java.lang.Math;

/**
 * The L1 cache. Each processor will have their own cache.
 *
 * Created by Bjorn Lim on 25/10/16.
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

    private FixedList<FixedQueue<ValidDirtyTag>> l1cache;
    private int associativity;
    private int arraySize;
    private int offsetBits;
    private int indexBits;
    private int tagBits;

    private int cacheMisses;
    private int cacheAccesses;


    public Cache (int cacheSize, int associativity, int blockSize) {
        cacheMisses = 0;
        cacheAccesses = 0;
        this.associativity = associativity;
        arraySize = cacheSize/blockSize/associativity;              // this is the number of "rows" in the cache
        l1cache = new FixedList<>(arraySize);                       // create a fixed size ArrayList
        for (int i = 0; i < arraySize; i++) {
            l1cache.set(i, new FixedQueue<>(associativity));        // create fixed size queue for associativity (LRU)
        }
        offsetBits = binaryLog(blockSize);
        indexBits = binaryLog(cacheSize/blockSize);
        tagBits = 32 - offsetBits - indexBits;

    }

    public boolean ldrInstruction (int address) {
        int index = getIndex(address);
        return checkRowForTag(l1cache.get(index), getTag(address));
    }

    public boolean strInstruction (int address) {
        int index = getIndex(address);
        return setTagInRow(l1cache.get(index), getTag(address));
    }

    private boolean checkRowForTag (FixedQueue<ValidDirtyTag> row, int tag) {
        cacheAccesses += 1;
        // check if the tag exists in the cache
        for (int i = 0; i < associativity; i++) {
            if (tag == row.get(i).tag && row.get(i).valid == true) {
                row.remove(i);
                row.add(new ValidDirtyTag(true, false, tag));
                return false;
            }
        }
        cacheMisses += 1;
        row.add(new ValidDirtyTag(true, false, tag));
        return true;
    }

    private boolean setTagInRow (FixedQueue<ValidDirtyTag> row, int tag) {
        cacheAccesses += 1;
        return true;
    }

    /**
     * @param number
     * @return log base 2 of the input integer
     */
    private static int binaryLog (int number) {
        return (int)Math.round(Math.log((double)number) / Math.log((double)2));
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
