package ru.hse.colorshare.util;

public class LimitGenerator<T> implements Generator<T> {
    private final Supplier<T> supplier;
    private long current = 0;
    private final long limit;

    public LimitGenerator(Supplier<T> supplier, long limit) {
        this.supplier = supplier;
        this.limit = limit;
    }

    @Override
    public boolean hasMore() {
        return current < limit;
    }

    @Override
    public T get() {
        current++;
        return supplier.get();
    }
}
