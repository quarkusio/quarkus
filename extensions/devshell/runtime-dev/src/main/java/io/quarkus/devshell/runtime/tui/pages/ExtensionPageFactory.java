package io.quarkus.devshell.runtime.tui.pages;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.devshell.runtime.spi.ShellPageProvider;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.ShellPageInfo;

/**
 * Factory for creating extension-specific pages.
 * First checks for registered ShellPageProviders, then falls back to
 * hardcoded pages or generic pages.
 */
public class ExtensionPageFactory {

    private static final Logger LOG = Logger.getLogger(ExtensionPageFactory.class);

    /**
     * Create a page for the given extension.
     * Checks for registered custom pages first, then providers, then falls back to
     * hardcoded pages or generic pages.
     */
    public static ExtensionPage createPage(ShellExtension extension, Map<String, ShellPageInfo> shellPages) {
        String namespace = extension.namespace();

        LOG.debugf("Creating page for extension: namespace=%s, name=%s, shellPages count=%d",
                namespace, extension.name(), shellPages != null ? shellPages.size() : 0);

        if (shellPages != null && namespace != null) {
            for (ShellPageInfo pageInfo : shellPages.values()) {
                boolean matches = matchesPage(namespace, pageInfo);
                LOG.debugf("  Checking pageInfo: id=%s, customPageClass=%s, matches=%s",
                        pageInfo.id(), pageInfo.customPageClassName(), matches);

                if (matches) {
                    // First, try custom page class
                    if (pageInfo.hasCustomPage()) {
                        LOG.debugf("  Attempting to create custom page: %s", pageInfo.customPageClassName());
                        ExtensionPage page = createCustomPage(extension, pageInfo);
                        if (page != null) {
                            LOG.debugf("  Successfully created custom page for %s", namespace);
                            return page;
                        }
                        LOG.debugf("  Custom page creation returned null for %s", namespace);
                    }
                    // Then, try provider
                    if (pageInfo.hasProvider()) {
                        LOG.debugf("  Attempting to create provider page: %s", pageInfo.providerClassName());
                        ExtensionPage page = createProviderPage(extension, pageInfo);
                        if (page != null) {
                            LOG.debugf("  Successfully created provider page for %s", namespace);
                            return page;
                        }
                        LOG.debugf("  Provider page creation returned null for %s", namespace);
                    }
                }
            }
        }

        // Fall back to hardcoded pages for backwards compatibility
        LOG.debugf("  Falling back to default page for %s", namespace);
        return createFallbackPage(extension);
    }

    /**
     * Match an extension namespace (from Dev UI, e.g. "quarkus-agroal") against a shell page id
     * (auto-detected from artifact, e.g. "agroal"). Both sides are normalized by stripping the
     * "quarkus-" prefix so they can be compared directly.
     */
    private static boolean matchesPage(String namespace, ShellPageInfo pageInfo) {
        String normalizedNamespace = stripQuarkusPrefix(namespace.toLowerCase());
        String normalizedPageId = stripQuarkusPrefix(pageInfo.id().toLowerCase());

        boolean matches = normalizedNamespace.equals(normalizedPageId);
        LOG.debugf("    matchesPage: namespace=%s, pageId=%s, normalized=[%s vs %s] -> %s",
                namespace, pageInfo.id(), normalizedNamespace, normalizedPageId, matches ? "MATCH" : "NO MATCH");
        return matches;
    }

    private static String stripQuarkusPrefix(String value) {
        if (value.startsWith("quarkus-")) {
            return value.substring("quarkus-".length());
        }
        return value;
    }

    /**
     * Create a custom ExtensionPage implementation.
     * Since all devshell classes are in runtime-dev, they share the same classloader
     * with extension shell pages, so direct instantiation works.
     */
    private static ExtensionPage createCustomPage(ShellExtension extension, ShellPageInfo pageInfo) {
        String pageClassName = pageInfo.customPageClassName();
        LOG.debugf("Creating custom page: class=%s for extension=%s", pageClassName, extension.name());

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            @SuppressWarnings("unchecked")
            Class<? extends ExtensionPage> pageClass = (Class<? extends ExtensionPage>) classLoader
                    .loadClass(pageClassName);

            // Try CDI first — supports @Inject in pages
            var container = Arc.container();
            if (container != null) {
                var handle = container.instance(pageClass);
                if (handle.isAvailable()) {
                    ExtensionPage page = handle.get();
                    if (page instanceof BaseExtensionPage) {
                        ((BaseExtensionPage) page).setExtension(extension);
                    }
                    LOG.debugf("  Created page via CDI: %s", page.getClass().getName());
                    return page;
                }
            }

            // Fallback: reflective instantiation with ShellExtension constructor
            ExtensionPage page = pageClass.getDeclaredConstructor(ShellExtension.class).newInstance(extension);
            LOG.debugf("  Created page via reflection: %s", page.getClass().getName());
            return page;

        } catch (ClassNotFoundException e) {
            LOG.errorf("Failed to load custom page class '%s' for %s. " +
                    "Ensure the class is in the runtime-dev classpath.",
                    pageClassName, extension.name());
            LOG.debugf(e, "ClassNotFoundException details for %s", pageClassName);
            return null;
        } catch (NoSuchMethodException e) {
            LOG.errorf("Custom page class '%s' for %s does not have required constructor(ShellExtension). " +
                    "Add: public %s(ShellExtension extension) { super(extension); }",
                    pageClassName, extension.name(), pageClassName.substring(pageClassName.lastIndexOf('.') + 1));
            LOG.debugf(e, "NoSuchMethodException details for %s", pageClassName);
            return null;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create custom page '%s' for %s: %s",
                    pageClassName, extension.name(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ExtensionPage createProviderPage(ShellExtension extension, ShellPageInfo pageInfo) {
        String providerClassName = pageInfo.providerClassName();
        LOG.debugf("Creating provider page: class=%s for extension=%s", providerClassName, extension.name());

        try {
            Class<?> providerClass = Thread.currentThread().getContextClassLoader().loadClass(providerClassName);
            LOG.debugf("  Successfully loaded provider class: %s", providerClass.getName());

            // Try to get the provider from CDI
            InstanceHandle<?> handle = Arc.container().instance((Class<Object>) providerClass);
            if (handle.isAvailable()) {
                ShellPageProvider provider = (ShellPageProvider) handle.get();
                LOG.debugf("  Got provider from CDI: %s", provider.getClass().getName());
                return new ProviderBasedPage(extension, provider);
            }

            // Fallback: try to instantiate directly
            LOG.debugf("  Provider not available in CDI, instantiating directly");
            ShellPageProvider provider = (ShellPageProvider) providerClass.getDeclaredConstructor().newInstance();
            return new ProviderBasedPage(extension, provider);

        } catch (ClassNotFoundException e) {
            LOG.errorf("Failed to load provider class '%s' for %s. " +
                    "Ensure the class is in the runtime classpath.",
                    providerClassName, extension.name());
            LOG.debugf(e, "ClassNotFoundException details for %s", providerClassName);
            return null;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create provider page '%s' for %s: %s",
                    providerClassName, extension.name(), e.getMessage());
            return null;
        }
    }

    private static ExtensionPage createFallbackPage(ShellExtension extension) {
        String namespace = extension.namespace();
        if (namespace == null) {
            return new GenericExtensionPage(extension);
        }

        // Core devshell pages (kept in devshell extension)
        if (matchesNamespace(namespace, "devui-configuration", "quarkus-vertx-http")) {
            return new ConfigurationPage(extension);
        }

        // Other extensions should register their pages via ShellPageProvider
        // Fallback to generic page for extensions without registered providers
        return new GenericExtensionPage(extension);
    }

    private static boolean matchesNamespace(String namespace, String... patterns) {
        String lowerNamespace = namespace.toLowerCase();
        for (String pattern : patterns) {
            if (lowerNamespace.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
