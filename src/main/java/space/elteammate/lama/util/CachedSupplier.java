package space.elteammate.lama.util;

import java.util.function.Supplier;

public class CachedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private T res = null;
    private boolean computed = false;

    public CachedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (!computed) {
            res = supplier.get();
            computed = true;
        }
        return res;
    }
}
