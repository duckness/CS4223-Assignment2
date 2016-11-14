/**
 * Encode the current state of the cache.
 */

public enum State {
    MODIFIED,       // AKA dirty
    SHARED_CLEAN,   // SHARED for MSI, MESI
    INVALID,        // does not exist for Dragon
    EXCLUSIVE,      // does not exist for MSI
    SHARED_DIRTY;   // does not exist for MSI, MESI
}
