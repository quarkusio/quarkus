package io.quarkus.hibernate.orm.runtime.boot.scan;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

// TODO Luca backported fromn Hibernate 7.x just to make it compile
public class UrlInputStreamAccess implements InputStreamAccess, Serializable {
	private final URL url;

	public UrlInputStreamAccess(URL url) {
		this.url = url;
	}

	@Override
	public String getStreamName() {
		return url.toExternalForm();
	}

	@Override
	public InputStream accessInputStream() {
		try {
			return url.openStream();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not open url stream : " + url.toExternalForm() );
		}
	}
}