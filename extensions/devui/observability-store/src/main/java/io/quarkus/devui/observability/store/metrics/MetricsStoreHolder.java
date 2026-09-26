package io.quarkus.devui.observability.store.metrics;

/**
 * Dev-mode holder that keeps the metrics store (and thus captured history and the current
 * selection) alive across live reloads. Like the traces holder, this class lives in an
 * immutable runtime jar loaded by the base runtime classloader, which is reused across
 * app-code reloads — so static state here outlives a reload even though the beans that use
 * it are recreated. Rebuilt only when the retention/capacity config changes (a deliberate
 * resize) or on a full dev restart / JVM exit.
 *
 * Only ever touched in dev mode (the capture adapters and JSON-RPC service that call it are
 * registered exclusively by dev-only build steps), so it adds no production footprint.
 */
public final class MetricsStoreHolder {

    private static MetricsTimeSeriesStore instance;
    private static long retentionMillis;
    private static int maxPointsPerSeries;
    private static long sampleIntervalMillis;

    private MetricsStoreHolder() {
    }

    public static synchronized MetricsTimeSeriesStore getOrCreate(long retentionMillis, int maxPointsPerSeries,
            long sampleIntervalMillis) {
        if (instance == null
                || MetricsStoreHolder.retentionMillis != retentionMillis
                || MetricsStoreHolder.maxPointsPerSeries != maxPointsPerSeries
                || MetricsStoreHolder.sampleIntervalMillis != sampleIntervalMillis) {
            instance = new MetricsTimeSeriesStore(retentionMillis, maxPointsPerSeries, sampleIntervalMillis);
            MetricsStoreHolder.retentionMillis = retentionMillis;
            MetricsStoreHolder.maxPointsPerSeries = maxPointsPerSeries;
            MetricsStoreHolder.sampleIntervalMillis = sampleIntervalMillis;
        }
        return instance;
    }
}
