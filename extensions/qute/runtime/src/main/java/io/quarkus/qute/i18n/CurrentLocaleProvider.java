package io.quarkus.qute.i18n;

import java.util.Locale;

/**
 * Provides the current locale used to resolve a message bundle injected with the {@link LocaleAware} qualifier.
 * <p>
 * Implementations must be CDI beans. Typically, an implementation resolves the locale from the {@code Accept-Language}
 * header of the current HTTP request. Integrations that expose a notion of "current request" (such as the REST
 * extensions) provide a built-in implementation.
 *
 * @see LocaleAware
 */
public interface CurrentLocaleProvider {

    /**
     *
     * @return the current locale, or {@code null} if it cannot be determined
     */
    Locale currentLocale();

}
