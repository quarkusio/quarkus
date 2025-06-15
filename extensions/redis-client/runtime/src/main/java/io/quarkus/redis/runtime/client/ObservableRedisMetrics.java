package io.quarkus.redis.runtime.client;

public interface ObservableRedisMetrics {

    /**
     * Method called by the {@link ObservableRedis} after every operation.
     *
     * @param name
     *        the client name
     * @param durationInNs
     *        the duration of the operation in ns, it can represent the execution of a single command or a batch.
     * @param succeeded
     *        whether the operation succeeded
     */
    void report(String name, long durationInNs, boolean succeeded);

    ObservableRedisMetrics NOOP = new ObservableRedisMetrics() {
        @Override
        public void report(String name, long durationInNs, boolean succeeded) {

        }
    };

}
