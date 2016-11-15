/**
 * Created by Eric on 15/11/2016.
 */

import java.util.Hashtable;

public class Processor {

    Cache cache;
    Protocol protocol;
    Instruction instruction;
    boolean isDone = false;
    int currentCycle = 0;
    Hashtable<String, Integer> currentInstr;
    int currentInstruction;
    int currentAddress;

    public Processor (int cacheSize, int blockSize, int associativity, Protocol proto, Instruction instr) {
        cache = new Cache(cacheSize, associativity, blockSize, proto);
        protocol = proto;
        instruction = instr;
    }

    public void getInstruction(int processorNum) {
        Hashtable<String, Integer> output = new Hashtable<>(2);
        output = instruction.getInstruction(processorNum);
        this.currentInstruction = output.get("instruction");
        this.currentAddress = output.get("address");
        System.out.println("Processor " + processorNum);
        System.out.println(currentInstruction + " " + currentAddress);
    }
}
