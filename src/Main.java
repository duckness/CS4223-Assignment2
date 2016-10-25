import java.io.FileNotFoundException;

public class Main {

    private static Instruction instructions;

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
        // set to all default values if lacking cache_size, associativity and/or block_size
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

        //testing if instructions work, print out the first 100 instructions (hint, it works)
        for (int j = 0; j < 100; j++) {
            for (int i = 0; i < 4; i++) {
                System.out.print("core " + i + ": ");
                System.out.println(instructions.getInstruction(i));
            }
        }
        /*
        switch(inputs[0].toUpperCase()) {
            case "MSI":
                //do something;
                break;
            case "MESI":
                //do something;
                break;
            case "DRAGON":
                //do something;
                break;
            default:
                System.out.println("Unrecognized protocol.");
                System.exit(1);
                break;
        }
        */
    }
}
