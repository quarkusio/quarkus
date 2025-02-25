package io.quarkus.micrometer.opentelemetry.deployment.common;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.resources.Resource;

public class MetricDataFilter {
    private Stream<MetricData> metricData;

    public MetricDataFilter(final InMemoryMetricExporter metricExporter, final String name) {
        metricData = metricExporter.getFinishedMetricItems()
                .stream()
                .filter(metricData -> metricData.getName().equals(name));
    }

    public MetricDataFilter route(final String route) {
        metricData = metricData.map(new Function<MetricData, MetricData>() {
            @Override
            public MetricData apply(final MetricData metricData) {
                return new MetricData() {
                    @Override
                    public Resource getResource() {
                        return metricData.getResource();
                    }

                    @Override
                    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
                        return metricData.getInstrumentationScopeInfo();
                    }

                    @Override
                    public String getName() {
                        return metricData.getName();
                    }

                    @Override
                    public String getDescription() {
                        return metricData.getDescription();
                    }

                    @Override
                    public String getUnit() {
                        return metricData.getUnit();
                    }

                    @Override
                    public MetricDataType getType() {
                        return metricData.getType();
                    }

                    @Override
                    public Data<?> getData() {
                        return new Data<PointData>() {
                            @Override
                            public Collection<PointData> getPoints() {
                                return metricData.getData().getPoints().stream().filter(new Predicate<PointData>() {
                                    @Override
                                    public boolean test(final PointData pointData) {
                                        String value = pointData.getAttributes().get(HTTP_ROUTE);
                                        return value != null && value.equals(route);
                                    }
                                }).collect(Collectors.toSet());
                            }
                        };
                    }
                };
            }
        });
        return this;
    }

    public MetricDataFilter path(final String path) {
        metricData = metricData.map(new Function<MetricData, MetricData>() {
            @Override
            public MetricData apply(final MetricData metricData) {
                return new MetricData() {
                    @Override
                    public Resource getResource() {
                        return metricData.getResource();
                    }

                    @Override
                    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
                        return metricData.getInstrumentationScopeInfo();
                    }

                    @Override
                    public String getName() {
                        return metricData.getName();
                    }

                    @Override
                    public String getDescription() {
                        return metricData.getDescription();
                    }

                    @Override
                    public String getUnit() {
                        return metricData.getUnit();
                    }

                    @Override
                    public MetricDataType getType() {
                        return metricData.getType();
                    }

                    @Override
                    public Data<?> getData() {
                        return new Data<PointData>() {
                            @Override
                            public Collection<PointData> getPoints() {
                                return metricData.getData().getPoints().stream().filter(new Predicate<PointData>() {
                                    @Override
                                    public boolean test(final PointData pointData) {
                                        String value = pointData.getAttributes().get(URL_PATH);
                                        return value != null && value.equals(path);
                                    }
                                }).collect(Collectors.toSet());
                            }
                        };
                    }
                };
            }
        });
        return this;
    }

    public MetricDataFilter tag(final String key, final String value) {
        return stringAttribute(key, value);
    }

    public MetricDataFilter stringAttribute(final String key, final String value) {
        metricData = metricData.map(new Function<MetricData, MetricData>() {
            @Override
            public MetricData apply(final MetricData metricData) {
                return new MetricData() {
                    @Override
                    public Resource getResource() {
                        return metricData.getResource();
                    }

                    @Override
                    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
                        return metricData.getInstrumentationScopeInfo();
                    }

                    @Override
                    public String getName() {
                        return metricData.getName();
                    }

                    @Override
                    public String getDescription() {
                        return metricData.getDescription();
                    }

                    @Override
                    public String getUnit() {
                        return metricData.getUnit();
                    }

                    @Override
                    public MetricDataType getType() {
                        return metricData.getType();
                    }

                    @Override
                    public Data<?> getData() {
                        return new Data<PointData>() {
                            @Override
                            public Collection<PointData> getPoints() {
                                return metricData.getData().getPoints().stream().filter(new Predicate<PointData>() {
                                    @Override
                                    public boolean test(final PointData pointData) {
                                        String v = pointData.getAttributes().get(AttributeKey.stringKey(key));
                                        boolean result = v != null && v.equals(value);
                                        if (!result) {
                                            System.out.println(
                                                    "\nNot Matching. Expected: " + key + " = " + value + " -> Found: " + v);
                                        }
                                        return result;
                                    }
                                }).collect(Collectors.toSet());
                            }
                        };
                    }
                };
            }
        });
        return this;
    }

    public List<MetricData> getAll() {
        return metricData.collect(Collectors.toList());
    }

    public MetricData lastReading() {
        return metricData.reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalArgumentException("Stream has no elements"));
    }

    public int lastReadingPointsSize() {
        return metricData.reduce((first, second) -> second)
                .map(data -> data.getData().getPoints().size())
                .orElseThrow(() -> new IllegalArgumentException("Stream has no elements"));
    }

    /**
     * Returns the first point data of the last reading.
     * Assumes only one data point can be present.
     *
     * @param pointDataClass
     * @param <T>
     * @return
     */
    public <T extends PointData> T lastReadingDataPoint(Class<T> pointDataClass) {
        List<T> list = lastReading().getData().getPoints().stream()
                .map(pointData -> (T) pointData)
                .toList();

        if (list.size() == 0) {
            throw new IllegalArgumentException("Stream has no elements");
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("Stream has more than one element");
        }
        return list.get(0);
    }

    public int countPoints(final MetricData metricData) {
        return metricData.getData().getPoints().size();
    }
}
