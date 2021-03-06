import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Vector;


public class Main {

    private static Instruction instructions;
    private static Vector<Processor> processors;

    private static boolean isAllComplete() {
        boolean processor0 = processors.elementAt(0).isProcDone();
        boolean processor1 = processors.elementAt(1).isProcDone();
        boolean processor2 = processors.elementAt(2).isProcDone();
        boolean processor3 = processors.elementAt(3).isProcDone();

        if(processor0 && processor1 && processor2 && processor3) {
            return true;
        }
        else {
            return false;
        }
    }

    private static void runProcessors() {
        int currentCycle = 0;
        while(!isAllComplete()) {
            for (int i = 0; i < 4; i++) {
                processors.elementAt(i).setClock(currentCycle);
                // need to do things for when instructions are complete OR when processor is stalled
                boolean a = processors.elementAt(i).isProcDone();
                boolean b = processors.elementAt(i).isProcStalled();
                if (processors.elementAt(i).isProcDone() || processors.elementAt(i).isProcStalled()) {
                    processors.elementAt(i).cacheBusSnoop();
                    continue;
                }
                processors.elementAt(i).getInstruction(i);
                processors.elementAt(i).executeInstruction(i);
            }
            // for debugging
            if (currentCycle%100000000 == 0) {
                System.out.println("cycle " + currentCycle);
            }
            Bus.runBusTransactions(currentCycle);
            currentCycle += 1;
        }
    }

    private static void calculateResults() {
        // amount of data traffic busread/buswrite/busupdate
        int traffic = 0;
        // number of invalidations/updates
        int update = 0;
        // distribution of private/shared data
        int privateData = 0;
        int sharedData = 0;
        // average write latency
        int hitSelf = 0;
        int hitOther = 0;
        int hitMemory = 0;

        for (int i = 0; i < 4; i++) {
            Hashtable<String, Integer> thisCache = processors.elementAt(i).getCacheResults();
            traffic += thisCache.get("traffic");
            update += thisCache.get("update");
            privateData += thisCache.get("private");
            sharedData += thisCache.get("shared");
            hitSelf += thisCache.get("hitSelf");
            hitOther += thisCache.get("hitOther");
            hitMemory += thisCache.get("hitMemory");
        }
        System.out.println("Total amount of data traffic (bytes):  " + traffic);
        System.out.println("Total number of invalidations/updates: " + update);
        System.out.println("Private data accesses(self): " + privateData);
        System.out.println("Shared data accesses(self):  " + sharedData);
        int memoryCycles;
        if (Bus.protocol == Protocol.MSI || Bus.protocol == Protocol.MESI) {
            memoryCycles = Bus.cycles_block;
        } else {
            memoryCycles = Bus.CYCLES_WORD;
        }
        double averageLatency = (double)(hitOther*memoryCycles + hitMemory*100) / (double)(hitSelf+hitOther+hitMemory);
        System.out.println("Average latency (number of clock cycles): " + averageLatency);
    }

    /**
     * String[0] “protocol”
     * String[1] “input_file”
     * String[2] “cache_size”
     * String[3] “associativity”
     * String[4] “block_size”
     */
    public static void main(String[] args) throws FileNotFoundException {
        String[] inputs = new String[5];
        inputs[0] = args[0];
        inputs[1] = args[1];
        // set to all default values if lacking cacheSize, associativity and/or blockSize
        if (args.length < 5) {
            System.out.println("Insufficient inputs, using default values.");
            inputs[2] = "4096";
            inputs[3] = "1";
            inputs[4] = "16";
        } else {
            inputs[2] = args[2];
            inputs[3] = args[3];
            inputs[4] = args[4];
        }

        instructions = new Instruction(inputs[1]);
        processors = new Vector<>();
        int cacheSize = Integer.parseInt(inputs[2]);
        int associativity = Integer.parseInt(inputs[3]);
        int blockSize = Integer.parseInt(inputs[4]);


        //testing if instructions work, print out the first 100 instructions (hint, it works)
        /*
        for (int j = 0; j < 1000; j++) {
            System.out.println("Cycle " + (j+1));
            for (int i = 0; i < 4; i++) {
                System.out.print("processor " + i + ": ");
                System.out.println(instructions.getInstruction(i));
            }
        }*/

        switch(inputs[0].toUpperCase()) {
            case "MSI":
                Bus.initBus(Protocol.MSI, blockSize/4); // blocksize/4 = number of (32bit/4byte)words per block
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.MSI, instructions, i));
                }
                runProcessors();
                calculateResults();
                break;

            case "MESI":
                Bus.initBus(Protocol.MESI, blockSize/4);
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.MESI, instructions, i));
                }
                runProcessors();
                calculateResults();
                break;

            case "DRAGON":
                Bus.initBus(Protocol.DRAGON, blockSize/4);
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.DRAGON, instructions, i));
                }
                runProcessors();
                calculateResults();
                break;

            default:
                System.out.println("Unrecognized protocol.");
                System.exit(1);
                break;
        }
    }
}
