package io.quarkus.agroal.runtime;

import java.util.Map;

public class DataSourceSupport {

    // everything needs to be mutable because it's used in a recorder

    public boolean disableSslSupport;
    public boolean mpMetricsPresent;
    public Map<String, Entry> entries;

    public DataSourceSupport() {
    }

    public DataSourceSupport(boolean disableSslSupport, boolean mpMetricsPresent, Map<String, Entry> entries) {
        this.disableSslSupport = disableSslSupport;
        this.mpMetricsPresent = mpMetricsPresent;
        this.entries = entries;
    }

    public static class Entry {
        public String dataSourceName;
        public String resolvedDbKind;
        public String resolvedDriverClass;
        public boolean isDefault;

        public Entry() {
        }

        public Entry(String dataSourceName, String resolvedDbKind, String resolvedDriverClass,
                boolean isDefault) {
            this.dataSourceName = dataSourceName;
            this.resolvedDbKind = resolvedDbKind;
            this.resolvedDriverClass = resolvedDriverClass;
            this.isDefault = isDefault;
        }
    }
}
