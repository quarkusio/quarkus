package io.quarkus.qute.runtime.i18n;

import java.util.List;
import java.util.Locale;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.qute.i18n.CurrentLocaleProvider;
import io.vertx.core.http.HttpServerRequest;

/**
 * The default {@link CurrentLocaleProvider} that resolves the current locale from the {@code Accept-Language} header of
 * the current HTTP request.
 * <p>
 * The current {@link HttpServerRequest} is injected optionally - it's only available if the {@code quarkus-vertx-http}
 * extension is present and a request context is active. Otherwise, {@code null} is returned and the default locale is
 * used.
 * <p>
 * This bean is a {@link DefaultBean} so that it can be replaced by a custom {@link CurrentLocaleProvider}.
 */
@DefaultBean
@Singleton
public class HttpServerRequestLocaleProvider implements CurrentLocaleProvider {

    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    @Inject
    Instance<HttpServerRequest> request;

    @Override
    public Locale currentLocale() {
        if (!Arc.container().requestContext().isActive() || !request.isResolvable()) {
            return null;
        }
        String acceptLanguage = request.get().getHeader(ACCEPT_LANGUAGE);
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        List<Locale.LanguageRange> ranges;
        try {
            // Parses the header value and sorts the language ranges by the weight (q-value)
            ranges = Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException e) {
            // Malformed header
            return null;
        }
        for (Locale.LanguageRange range : ranges) {
            String languageRange = range.getRange();
            if (!"*".equals(languageRange)) {
                return Locale.forLanguageTag(languageRange);
            }
        }
        return null;
    }

}
