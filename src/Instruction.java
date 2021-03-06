/**
 * class to read and interpret the .data files.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

public class Instruction {

    private List<FileInputStream> inputStreams;
    private List<Scanner> scs;
    // stores a buffer of instructions if required
    private List<Integer> buffers;
    private int[] currentLine;

    public Instruction(String arg) throws FileNotFoundException {
        currentLine = new int[] {0,0,0,0};
        inputStreams = new ArrayList<>(4);
        scs = new ArrayList<>(4);
        buffers = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            inputStreams.add(new FileInputStream(arg.toLowerCase() + "_" + i + ".data"));
            scs.add(new Scanner(inputStreams.get(i), "UTF-8"));
            buffers.add(null);
        }
    }

    /**
     * @param processor is the processor that you want to get the instruction for
     * @return a Hashtable of size 2 for the current instruction
     *
     * "instruction"
     * is type of instruction
     * LDR - 0
     * STR - 1
     * NOP - 2
     *
     * "address"
     * is the address (NOP stores as the number of NOPs left)
     *
     * Will return -1 in both fields if out of instructions
     */
    public Hashtable<String, Integer> getInstruction(int processor) {
        Hashtable<String, Integer> output = new Hashtable<>(2);
        String line;

        // check if it is a NOP instruction that has yet to be executed
        if (buffers.get(processor) != null) {
            output.put("instruction", 2);
            output.put("address", buffers.get(processor));
            if (buffers.get(processor) == 0) {
                buffers.set(processor, null);
            } else {
                int oldValue = buffers.get(processor) - 1;
                buffers.set(processor, oldValue);
            }
            // else grab a new instruction from the file
        } else {
            // for debugging
            currentLine[processor] += 1;
            // get instructions from new line if exists
            if (scs.get(processor).hasNextLine()) {
                line = scs.get(processor).nextLine();
                String[] parts = line.split(" ");
                output.put("instruction", Integer.decode(parts[0]));
                output.put("address", Integer.decode(parts[1]));

                // set buffer for NOPs
                if (Integer.decode(parts[0]) == 2) {
                    buffers.set(processor, Integer.decode(parts[1]) - 1);
                }
                // otherwise, return -1
            } else {
                output.put("instruction", -1);
                output.put("address", -1);
            }
        }
        return output;
    }
}
