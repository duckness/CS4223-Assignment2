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
        //System.out.println("Processor " + processorNum);
        //System.out.println(currentInstruction + " " + currentAddress);
    }

    public void executeInstruction(int processorNum) {
        switch (this.currentInstruction) {
            case 0:
                this.currentCycle++;
                //System.out.println("Processor " + processorNum + " current cycle is " + this.currentCycle + " executing Load");
                //execute load instruction below
                break;
            case 1:
                this.currentCycle++;
                //System.out.println("Processor " + processorNum + " current cycle is " + this.currentCycle + " executing Store");
                //execute store instruction below
                break;
            case 2:
                this.currentCycle++;
                // do nothing for NOP instruction
                //System.out.println("Processor " + processorNum + " current cycle is " + this.currentCycle);
                break;
            case -1:
                this.isDone = true;
                System.out.println("Processor " + processorNum + " has no more instructions");
                System.out.println("Processor " + processorNum + "'s final cycle is " + this.currentCycle);
                break;
            default:
                System.out.println("something wrong in executeInstruction function at Processor class");
                break;
        }
    }

    public boolean isProcDone() {
        return this.isDone;
    }
}
