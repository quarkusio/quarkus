package io.quarkus.reactive.datasource.runtime;

final class TestableThreadLocalPool extends ThreadLocalPool<TestPool> {

    public TestableThreadLocalPool() {
        super(null, null);
    }

    @Override
    protected TestPool createThreadLocalPool() {
        return new TestPool();
    }

}
