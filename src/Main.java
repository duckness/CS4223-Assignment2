import java.io.FileNotFoundException;
import java.util.Vector;


public class Main {

    private static Instruction instructions;
    private static Vector<Processor> processors;

    static boolean isAllComplete() {
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
        processors = new Vector<Processor>();
        int cacheSize = Integer.parseInt(inputs[2]);
        int associativity = Integer.parseInt(inputs[3]);
        int blockSize = Integer.parseInt(inputs[4]);

        /*
        //testing if instructions work, print out the first 100 instructions (hint, it works)
        for (int j = 0; j < 100; j++) {
            System.out.println("Cycle " + (j+1));
            for (int i = 0; i < 4; i++) {
                System.out.print("processor " + i + ": ");
                System.out.println(instructions.getInstruction(i));
            }
        }
        */



        switch(inputs[0].toUpperCase()) {
            case "MSI":
                Bus.initBus(Protocol.MSI);
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.MSI, instructions, i));
                }

                while(!isAllComplete()) {
                    for (int i = 0; i < 4; i++) {
                        processors.elementAt(i).getInstruction(i);
                        processors.elementAt(i).executeInstruction(i);
                        Bus.runBusTransactions();
                    }
                }
                break;
            case "MESI":
                Bus.initBus(Protocol.MESI);
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.MESI, instructions, i));
                }

                while(!isAllComplete()) {
                    for (int i = 0; i < 4; i++) {
                        processors.elementAt(i).getInstruction(i);
                        processors.elementAt(i).executeInstruction(i);
                        Bus.runBusTransactions();
                    }
                }
                break;
            case "DRAGON":
                Bus.initBus(Protocol.DRAGON);
                for (int i = 0; i < 4; i++) {
                    System.out.println(i);
                    processors.add(new Processor(cacheSize, blockSize, associativity, Protocol.DRAGON, instructions, i));
                }

                while(!isAllComplete()) {
                    for (int i = 0; i < 4; i++) {
                        processors.elementAt(i).getInstruction(i);
                        processors.elementAt(i).executeInstruction(i);
                        Bus.runBusTransactions();
                    }
                }
                break;
            default:
                System.out.println("Unrecognized protocol.");
                System.exit(1);
                break;
        }
    }
}
