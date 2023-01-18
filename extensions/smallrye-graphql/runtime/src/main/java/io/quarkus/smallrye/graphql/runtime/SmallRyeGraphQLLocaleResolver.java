package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;

import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;

/**
 * Resolving BV messages for SmallRye GraphQL
 */
@Singleton
public class SmallRyeGraphQLLocaleResolver implements LocaleResolver {

    private static final String ACCEPT_HEADER = "Accept-Language";

    @Override
    public Locale resolve(LocaleResolverContext context) {
        Optional<List<Locale.LanguageRange>> localePriorities = getAcceptableLanguages();
        if (!localePriorities.isPresent()) {
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
                return Optional.of(Locale.LanguageRange.parse(acceptLanguageList.get(0)));
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getHeaders() {
        SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        if (smallRyeContext != null) {
            DataFetchingEnvironment dfe = smallRyeContext.unwrap(DataFetchingEnvironment.class);
            return (Map<String, List<String>>) dfe.getGraphQlContext().get("httpHeaders");
        } else {
            return null;
        }
    }
}
