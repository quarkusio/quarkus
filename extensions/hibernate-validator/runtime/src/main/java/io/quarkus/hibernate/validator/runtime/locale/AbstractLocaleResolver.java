package io.quarkus.hibernate.validator.runtime.locale;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;

abstract class AbstractLocaleResolver implements LocaleResolver {

    protected abstract Map<String, List<String>> getHeaders();

    @Override
    public Locale resolve(LocaleResolverContext context) {
        Optional<List<Locale.LanguageRange>> localePriorities = getAcceptableLanguages();
        if (!localePriorities.isPresent()) {
            return null;
        }

        List<Locale> resolvedLocales = Locale.filter(localePriorities.get(), context.getSupportedLocales());
        if (resolvedLocales.size() > 0) {
            return resolvedLocales.get(0);
        }

        return null;
    }

    private Optional<List<Locale.LanguageRange>> getAcceptableLanguages() {
        Map<String, List<String>> httpHeaders = getHeaders();
        if (httpHeaders != null) {
            List<String> acceptLanguageList = httpHeaders.get("Accept-Language");
            if (acceptLanguageList != null && !acceptLanguageList.isEmpty()) {
                return Optional.of(Locale.LanguageRange.parse(acceptLanguageList.get(0)));
            }
        }

        return Optional.empty();
    }
}
