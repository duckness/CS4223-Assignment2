/**
 * Created by Eric on 15/11/2016.
 */

import java.util.Hashtable;

public class Processor {

    Cache cache;
    Protocol protocol;
    Instruction instruction;

    public Processor (int cacheSize, int blockSize, int associativity, Protocol proto, Instruction instr) {
        cache = new Cache(cacheSize, associativity, blockSize, proto);
        protocol = proto;
        instruction = instr;
    }
}
