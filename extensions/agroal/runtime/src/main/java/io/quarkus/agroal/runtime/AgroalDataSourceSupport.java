package io.quarkus.agroal.runtime;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
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

    public static class Entry implements Constable {

        private static final ClassDesc CD_ENTRY = ClassDesc.of(Entry.class.getName());
        private static final DirectMethodHandleDesc MH_OF = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC,
                CD_ENTRY, "of", MethodTypeDesc.of(CD_ENTRY, ConstantDescs.CD_String, ConstantDescs.CD_String,
                        ConstantDescs.CD_String, ConstantDescs.CD_String, ConstantDescs.CD_boolean));

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

        public static Entry of(String dataSourceName, String resolvedDbKind, String dbVersion, String resolvedDriverClass,
                boolean isDefault) {
            return new Entry(dataSourceName, resolvedDbKind, Optional.ofNullable(dbVersion), resolvedDriverClass, isDefault);
        }

        @Override
        public Optional<? extends ConstantDesc> describeConstable() {
            return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "entry", CD_ENTRY, MH_OF,
                    dataSourceName, resolvedDbKind, dbVersion.isPresent() ? dbVersion.get() : ConstantDescs.NULL,
                    resolvedDriverClass, isDefault ? ConstantDescs.TRUE : ConstantDescs.FALSE));
        }
    }
}
