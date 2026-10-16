package io.quarkus.devui.tests;

import static io.quarkus.devui.tests.DevUITestUtils.LOCAL_BASE_URI;
import static io.quarkus.devui.tests.DevUITestUtils.managementRootPath;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;

/**
 * Base class for tests that load the Dev UI in a headless Chromium using Playwright.
 * <p>
 * Tests extending this class must add {@code com.microsoft.playwright:playwright} as a test dependency.
 * Errors reported by the page (uncaught exceptions and console errors) are collected in {@link #pageErrors()}.
 */
public abstract class DevUIBrowserTest {

    private static Playwright playwright;
    private static Browser browser;

    private final List<String> pageErrors = new CopyOnWriteArrayList<>();
    private URI devUIUri;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void stopBrowser() {
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @BeforeEach
    void openPage(ValueRegistry valueRegistry) {
        URI localBaseUri = valueRegistry.containsKey(LOCAL_BASE_URI)
                ? valueRegistry.get(LOCAL_BASE_URI)
                : URI.create("http://localhost:8080");
        devUIUri = localBaseUri.resolve(managementRootPath(Config.get(), "/dev-ui/"));
        newPage();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * Closes the current page and opens a new one, discarding the errors collected so far.
     * <p>
     * Use this instead of clearing {@link #pageErrors()}, as the previous page could still report errors.
     */
    protected Page reopenPage() {
        closePage();
        newPage();
        return page;
    }

    private void newPage() {
        pageErrors.clear();
        context = browser.newContext();
        page = context.newPage();
        page.onPageError(pageErrors::add);
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                pageErrors.add(message.text());
            }
        });
    }

    /**
     * Navigates to the Dev UI landing page.
     */
    protected Page openDevUI() {
        page.navigate(devUIUri.toString());
        return page;
    }

    /**
     * Returns the card of an extension on the Extensions page, by extension name.
     */
    protected Locator extensionCard(String name) {
        return page.locator("qwc-extension[name='" + name + "']");
    }

    /**
     * Returns the menu item of a page, by the name of the web component that renders the page.
     */
    protected Locator menuItem(String componentName) {
        return page.locator("qwc-menu span.item-text[data-page='" + componentName + "']").locator("xpath=..");
    }

    /**
     * Returns the base URI the application is listening on.
     */
    protected URI baseUri() {
        return devUIUri.resolve("/");
    }

    protected Page page() {
        return page;
    }

    protected List<String> pageErrors() {
        return pageErrors;
    }
}
