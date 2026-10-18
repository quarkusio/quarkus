package io.quarkus.hibernate.validator.runtime.locale;

import java.util.Locale;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolverContext;

/**
 * Wrapper for potentially multiple locale resolvers. The first one that actually returns a non-null Locale will be used.
 */
@Singleton
@Named("locale-resolver-wrapper")
public class LocaleResolversWrapper implements LocaleResolver {

    @Inject
    Instance<LocaleResolver> resolvers;

    @Override
    public Locale resolve(LocaleResolverContext context) {
        for (LocaleResolver resolver : resolvers) {
            if (!resolver.equals(this)) {
                Locale locale = resolver.resolve(context);
                if (locale != null) {
                    return locale;
                }
            }
        }
        return context.getDefaultLocale();
    }

}
