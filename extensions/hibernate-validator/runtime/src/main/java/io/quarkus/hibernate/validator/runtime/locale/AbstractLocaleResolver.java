package io.quarkus.hibernate.validator.runtime.locale;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

abstract class AbstractLocaleResolver implements LocaleResolver {

    private static final Logger log = Logger.getLogger(AbstractLocaleResolver.class);
    private static final String ACCEPT_HEADER = "Accept-Language";

    /**
     * @return case-insensitive map
     * @see CaseInsensitiveMap
     */
    protected abstract Map<String, List<String>> getHeaders();

    @Override
    public Locale resolve(LocaleResolverContext context) {
        Optional<List<Locale.LanguageRange>> localePriorities = getAcceptableLanguages();
        if (localePriorities.isEmpty()) {
            return null;
        }
        List<Locale> resolvedLocales = Locale.filter(localePriorities.get(), context.getSupportedLocales());
        if (!resolvedLocales.isEmpty()) {
            return resolvedLocales.get(0);
        }

        return null;
    }

    private Optional<List<Locale.LanguageRange>> getAcceptableLanguages() {
        Map<String, List<String>> httpHeaders = getHeaders();
        if (httpHeaders != null) {
            List<String> acceptLanguageList = httpHeaders.get(ACCEPT_HEADER);
            if (acceptLanguageList != null && !acceptLanguageList.isEmpty()) {
                try {
                    return Optional.of(Locale.LanguageRange.parse(acceptLanguageList.get(0)));
                } catch (IllegalArgumentException e) {
                    // this can happen when parsing malformed locale range string
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to parse the \"Accept-Language\" header. \"" + acceptLanguageList.get(0)
                                + "\" is not a valid language range string.", e);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
