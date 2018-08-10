package org.hibernate.protean.recording;

import java.util.Map;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;

public final class RecordedState {

	private final Dialect dialect;
	private final MetadataImplementor fullMeta;
	private Map<String, Object> cfg;

	public RecordedState(Dialect dialect, MetadataImplementor fullMeta, Map<String, Object> cfg) {
		this.dialect = dialect;
		this.fullMeta = fullMeta;
		this.cfg = cfg;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public MetadataImplementor getFullMeta() {
		return fullMeta;
	}

	public Map<String, Object> getConfigurationProperties() {
		return cfg;
	}

}
