package io.quarkus.agroal.metrics;

import org.eclipse.microprofile.metrics.Gauge;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.quarkus.arc.Arc;

public class AgroalGauge implements Gauge<Long> {

    private String dataSourceName;
    private volatile AgroalDataSource dataSource;
    private String metric;

    public AgroalGauge() {

    }

    /**
     * @param dataSourceName Which datasource should be queried for metric
     * @param metricName Name of the method from DataSource.getMetrics() that should be called to retrieve the particular value.
     *        This has nothing to do with the metric name from MP Metrics point of view!
     */
    public AgroalGauge(String dataSourceName, String metricName) {
        this.dataSourceName = dataSourceName;
        this.metric = metricName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
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

    @Override
    public Long getValue() {
        AgroalDataSourceMetrics metrics = getDataSource().getMetrics();
        switch (metric) {
            case "activeCount":
                return metrics.activeCount();
            case "availableCount":
                return metrics.availableCount();
            case "maxUsedCount":
                return metrics.maxUsedCount();
            case "awaitingCount":
                return metrics.awaitingCount();
            case "blockingTimeAverage":
                return metrics.blockingTimeAverage().toMillis();
            case "blockingTimeMax":
                return metrics.blockingTimeMax().toMillis();
            case "blockingTimeTotal":
                return metrics.blockingTimeTotal().toMillis();
            case "creationTimeAverage":
                return metrics.creationTimeAverage().toMillis();
            case "creationTimeMax":
                return metrics.creationTimeMax().toMillis();
            case "creationTimeTotal":
                return metrics.creationTimeTotal().toMillis();
            default:
                throw new IllegalArgumentException("Unknown data source metric");
        }
    }
}
