package org.hibernate.protean.recording;

import java.util.Map;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public final class RecordedState {

	private final Dialect dialect;
	private final MetadataImplementor fullMeta;
	private final JtaPlatform jtaPlatform;
	private final Map<String, Object> cfg;

	public RecordedState(
			Dialect dialect,
			JtaPlatform jtaPlatform,
			MetadataImplementor fullMeta,
			Map<String, Object> cfg) {
		this.dialect = dialect;
		this.jtaPlatform = jtaPlatform;
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

	public JtaPlatform getJtaPlatform() {
		return jtaPlatform;
	}
}
