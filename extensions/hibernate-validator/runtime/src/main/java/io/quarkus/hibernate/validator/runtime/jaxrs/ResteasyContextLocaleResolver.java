package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Optional;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;
import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class ResteasyContextLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolve(LocaleResolverContext context) {
        Optional<List<LanguageRange>> localePriorities = getAcceptableLanguages();
        if (!localePriorities.isPresent()) {
            return context.getDefaultLocale();
        }

        List<Locale> resolvedLocales = Locale.filter(localePriorities.get(), context.getSupportedLocales());
        if (resolvedLocales.size() > 0) {
            return resolvedLocales.get(0);
        }

        return context.getDefaultLocale();
    }

    private Optional<List<LanguageRange>> getAcceptableLanguages() {
        HttpHeaders httpHeaders = ResteasyContext.getContextData(HttpHeaders.class);
        if (httpHeaders != null) {
            List<String> acceptLanguageList = httpHeaders.getRequestHeader("Accept-Language");
            if (acceptLanguageList != null && !acceptLanguageList.isEmpty()) {
                return Optional.of(LanguageRange.parse(acceptLanguageList.get(0)));
            }
        }

        return Optional.empty();
    }

}
