package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class DataSourceSupport {

    public final boolean disableSslSupport;
    public final boolean mpMetricsPresent;
    public final Map<String, Entry> entries;

    @RecordableConstructor
    public DataSourceSupport(boolean disableSslSupport, boolean mpMetricsPresent, Map<String, Entry> entries) {
        this.disableSslSupport = disableSslSupport;
        this.mpMetricsPresent = mpMetricsPresent;
        this.entries = entries;
    }

    public static class Entry {
        public final String dataSourceName;
        public final String resolvedDbKind;
        public final String resolvedDriverClass;
        public final boolean isDefault;

        @RecordableConstructor
        public Entry(String dataSourceName, String resolvedDbKind, String resolvedDriverClass,
                boolean isDefault) {
            this.dataSourceName = dataSourceName;
            this.resolvedDbKind = resolvedDbKind;
            this.resolvedDriverClass = resolvedDriverClass;
            this.isDefault = isDefault;
        }
    }
}
