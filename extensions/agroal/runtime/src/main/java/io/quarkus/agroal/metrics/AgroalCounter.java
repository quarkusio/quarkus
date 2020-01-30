package io.quarkus.agroal.metrics;

import org.eclipse.microprofile.metrics.Counter;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.quarkus.arc.Arc;

public class AgroalCounter implements Counter {

    private String dataSourceName;
    private volatile AgroalDataSource dataSource;
    private String metric;

    public AgroalCounter() {

    }

    /**
     * @param dataSourceName Which datasource should be queried for metric
     * @param metricName Name of the method from DataSource.getMetrics() that should be called to retrieve the particular value.
     *        This has nothing to do with the metric name from MP Metrics point of view!
     */
    public AgroalCounter(String dataSourceName, String metricName) {
        this.dataSourceName = dataSourceName;
        this.metric = metricName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    private AgroalDataSource getDataSource() {
        AgroalDataSource dsLocal = dataSource;
        if (dsLocal == null) {
            synchronized (this) {
                dsLocal = dataSource;
                if (dsLocal == null) {
                    if (dataSourceName == null) {
                        dataSource = dsLocal = Arc.container().instance(AgroalDataSource.class).get();
                    } else {
                        dataSource = dsLocal = Arc.container()
                                .instance(AgroalDataSource.class, new DataSourceLiteral(dataSourceName))
                                .get();
                    }
                }
            }
        }
        return dsLocal;
    }

    public void setDataSource(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    @Override
    public void inc() {
    }

    @Override
    public void inc(long n) {
    }

    @Override
    public long getCount() {
        AgroalDataSourceMetrics metrics = getDataSource().getMetrics();
        switch (metric) {
            case "acquireCount":
                return metrics.acquireCount();
            case "creationCount":
                return metrics.creationCount();
            case "leakDetectionCount":
                return metrics.leakDetectionCount();
            case "destroyCount":
                return metrics.destroyCount();
            case "flushCount":
                return metrics.flushCount();
            case "invalidCount":
                return metrics.invalidCount();
            case "reapCount":
                return metrics.reapCount();
            default:
                throw new IllegalArgumentException("Unknown datasource metric");
        }
    }
}
