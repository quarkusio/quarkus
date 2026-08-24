package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifies an injected message bundle interface that should be resolved using the current locale, e.g. the locale negotiated
 * from the {@code Accept-Language} header of the current HTTP request.
 * <p>
 * Unlike the default message bundle bean, which is bound to the default locale at build time, a bean qualified with this
 * annotation selects the appropriate localized variant per method invocation. The current locale is obtained from a
 * {@link CurrentLocaleProvider} bean. If no provider is available, or the current locale cannot be determined, or no
 * matching localized variant exists, then the default locale is used as a fallback.
 *
 * <pre>
 * &#64;LocaleAware
 * AppMessages messages;
 * </pre>
 *
 * @see MessageBundle
 * @see Localized
 * @see CurrentLocaleProvider
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface LocaleAware {

    public static final class Literal extends AnnotationLiteral<LocaleAware> implements LocaleAware {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
