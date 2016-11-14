/**
 * Quick and dirty CacheBlock.
 */
public class CacheBlock {
    public State state;
    public int tag;

    public CacheBlock(State state, int tag) {
        this.state = state;
        this.tag = tag;
    }
}
