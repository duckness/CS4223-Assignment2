/**
 * A simple queue with fixed length.
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
