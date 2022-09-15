package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.ext.RuntimeDelegate;
import java.util.Locale;
import org.jboss.resteasy.reactive.common.util.LocaleHelper;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class LocaleDelegate implements RuntimeDelegate.HeaderDelegate<Locale> {
    public static final LocaleDelegate INSTANCE = new LocaleDelegate();

    public Locale fromString(String value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("param was null");
        return LocaleHelper.extractLocale(value);
    }

    public String toString(Locale value) {
        if (value == null)
            throw new IllegalArgumentException("param was null");
        return LocaleHelper.toLanguageString(value);
    }

}
