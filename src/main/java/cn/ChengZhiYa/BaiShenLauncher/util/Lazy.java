package cn.ChengZhiYa.BaiShenLauncher.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Non thread-safe lazy initialization wrapper.
 *
 * @param <T> value type
 */
public class Lazy<T> {
    private Supplier<T> supplier;
    private T value = null;

    public Lazy(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    public T get() {
        if (value == null) {
            value = Objects.requireNonNull(supplier.get());
            supplier = null;
        }
        return value;
    }

}
