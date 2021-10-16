package io.quarkus.registry;

import java.util.Optional;
import java.util.function.Supplier;

public interface ResolverState {

    static <T> Lazy<T> lazy(Supplier<T> function) {
        return new Lazy<>(function);
    }

    static <T> LazyResolver<T> lazyResolver(ResolvingSupplier<T> function) {
        return new LazyResolver<>(function);
    }

    class Lazy<T> {
        final Supplier<T> supplier;
        T instance;

        private Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (instance == null) {
                supplier.get();
            }
            return instance;
        }

        public Optional<T> test() {
            return Optional.of(instance);
        }
    }

    @FunctionalInterface
    interface ResolvingSupplier<T> {
        T get() throws RegistryResolutionException;
    }

    class LazyResolver<T> {
        final ResolvingSupplier<T> supplier;
        T instance;

        private LazyResolver(ResolvingSupplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() throws RegistryResolutionException {
            if (instance == null) {
                supplier.get();
            }
            return instance;
        }

        public Optional<T> test() {
            return Optional.of(instance);
        }
    }
}
