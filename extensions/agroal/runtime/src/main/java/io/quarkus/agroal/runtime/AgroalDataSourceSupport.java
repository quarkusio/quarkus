package io.quarkus.agroal.runtime;

import java.util.Map;
import java.util.Optional;

public class AgroalDataSourceSupport {

    public final boolean disableSslSupport;
    public final boolean mpMetricsPresent;
    public final Map<String, Entry> entries;

    public AgroalDataSourceSupport(boolean disableSslSupport, boolean mpMetricsPresent, Map<String, Entry> entries) {
        this.disableSslSupport = disableSslSupport;
        this.mpMetricsPresent = mpMetricsPresent;
        this.entries = entries;
    }

    public static class Entry {
        public final String dataSourceName;
        public final String resolvedDbKind;
        public final Optional<String> dbVersion;
        public final String resolvedDriverClass;
        public final boolean isDefault;

        public Entry(String dataSourceName, String resolvedDbKind, Optional<String> dbVersion,
                String resolvedDriverClass,
                boolean isDefault) {
            this.dataSourceName = dataSourceName;
            this.resolvedDbKind = resolvedDbKind;
            this.dbVersion = dbVersion;
            this.resolvedDriverClass = resolvedDriverClass;
            this.isDefault = isDefault;
        }
    }
}
