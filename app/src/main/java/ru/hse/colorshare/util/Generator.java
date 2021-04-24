package ru.hse.colorshare.util;

public interface Generator<T> extends Supplier<T> {

    boolean hasMore();

    default void forEach(Consumer<T> consumer) {
        while (hasMore()) {
            consumer.accept(get());
        }
    }
}
