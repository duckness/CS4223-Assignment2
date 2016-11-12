/**
 * Need to set a queue for the associativity zzz
 * Created by Bjorn on 12/11/16.
 */
import java.util.LinkedList;

public class FixedQueue<E> extends LinkedList<E> {
    private int limit;

    public FixedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) {
            super.remove();
        }
        return true;
    }
}
