package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;

abstract class AbstractLocaleResolver implements LocaleResolver {

    protected abstract HttpHeaders getHeaders();

    @Override
    public Locale resolve(LocaleResolverContext context) {
        Optional<List<Locale.LanguageRange>> localePriorities = getAcceptableLanguages();
        if (!localePriorities.isPresent()) {
            return context.getDefaultLocale();
        }

        List<Locale> resolvedLocales = Locale.filter(localePriorities.get(), context.getSupportedLocales());
        if (resolvedLocales.size() > 0) {
            return resolvedLocales.get(0);
        }

        return context.getDefaultLocale();
    }

    private Optional<List<Locale.LanguageRange>> getAcceptableLanguages() {
        HttpHeaders httpHeaders = getHeaders();
        if (httpHeaders != null) {
            List<String> acceptLanguageList = httpHeaders.getRequestHeader("Accept-Language");
            if (acceptLanguageList != null && !acceptLanguageList.isEmpty()) {
                return Optional.of(Locale.LanguageRange.parse(acceptLanguageList.get(0)));
            }
        }

        return Optional.empty();
    }
}
