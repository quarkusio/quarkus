package io.quarkus.test.devui.playwright;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.microsoft.playwright.Locator;

import io.quarkus.devui.tests.DevUIBrowserTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIPlaywrightTest extends DevUIBrowserTest {

    private static final String[] CORE_PAGES = {
            "qwc-extensions",
            "qwc-configuration",
            "qwc-continuous-testing",
            "qwc-endpoints"
    };

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(HelloResource.class));

    @Test
    public void testExtensionCardsRender() {
        openDevUI();

        assertThat(page().locator("qwc-extensions")).isVisible();
        assertThat(extensionCard("REST")).isVisible();
        assertThat(pageErrors()).isEmpty();
    }

    @Test
    public void testCorePagesRender() {
        openDevUI();

        for (String componentName : CORE_PAGES) {
            Locator menuItem = menuItem(componentName);
            assertThat(menuItem).isVisible();
            menuItem.click();
            assertThat(page().locator(componentName)).isVisible();
        }
        assertThat(pageErrors()).isEmpty();
    }

    @Test
    public void testDevUIAfterHotReload() {
        openDevUI();
        assertThat(extensionCard("REST")).isVisible();

        config.modifySourceFile(HelloResource.class, s -> s.replace("\"Hello\"", "\"Hi\""));
        assertThat(page().request().get(baseUri().resolve("/hello").toString()).text()).isEqualTo("Hi");

        // The page opened before the restart loses its Json-RPC connection, only check the reloaded page
        reopenPage();
        openDevUI();
        assertThat(extensionCard("REST")).isVisible();
        assertThat(pageErrors()).isEmpty();
    }
}
