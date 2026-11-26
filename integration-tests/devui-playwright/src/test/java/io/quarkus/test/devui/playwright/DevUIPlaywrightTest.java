package io.quarkus.test.devui.playwright;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import io.quarkus.devui.tests.DevUIBrowserTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIPlaywrightTest extends DevUIBrowserTest {

    private static final String[] CORE_PAGES = {
            "qwc-extensions",
            "qwc-configuration",
            "qwc-continuous-testing",
            "qwc-endpoints"
    };

    // Delays the Json-RPC responses the page receives
    private static final String SLOW_JSON_RPC = """
            const NativeWebSocket = window.WebSocket;
            window.WebSocket = class extends NativeWebSocket {
                set onmessage(handler) {
                    super.onmessage = (event) => setTimeout(() => handler.call(this, event), 2000);
                }
            };
            """;

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
    public void testConfigurationPageLoadedAfterLocale() {
        openDevUI();
        menuItem("qwc-configuration").click();
        assertThat(page().locator("qwc-configuration")).isVisible();
        String configurationUrl = page().url();

        // The locale is then ready before the configuration is loaded
        Page page = reopenPage();
        page.addInitScript(SLOW_JSON_RPC);
        page.navigate(configurationUrl);
        assertThat(page.locator("qwc-configuration vaadin-grid")).isVisible();
        assertThat(pageErrors()).isEmpty();
    }

    @Test
    public void testDevUIAfterHotReload() {
        openDevUI();
        assertThat(extensionCard("REST")).isVisible();

        config.modifySourceFile(HelloResource.class, s -> s.replace("\"Hello\"", "\"Hi\""));
        // The first request after the change can hit the application while it restarts
        await().atMost(Duration.ofSeconds(30)).ignoreExceptions()
                .untilAsserted(() -> assertThat(page().request().get(baseUri().resolve("hello").toString()).text())
                        .isEqualTo("Hi"));

        // The page opened before the restart loses its Json-RPC connection, only check the reloaded page
        reopenPage();
        openDevUI();
        assertThat(extensionCard("REST")).isVisible();
        assertThat(pageErrors()).isEmpty();
    }
}
