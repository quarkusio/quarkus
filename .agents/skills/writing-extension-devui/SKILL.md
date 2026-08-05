---
name: writing-extension-devui
description: >
  How to add a Dev UI page to a Quarkus extension: deployment processors,
  runtime-dev JSON-RPC services, and Lit web components.
---

# Writing a Dev UI for a Quarkus Extension

Dev UI is the interactive dashboard at `/q/dev-ui` during `quarkus:dev`.
Extensions add pages via build items, runtime JSON-RPC services, and Lit web
components. See the [Dev UI guide](https://quarkus.io/guides/dev-ui) for full
documentation.

## Directory Layout

```
my-extension/
  deployment/src/main/java/.../deployment/devui/
    MyFeatureDevUIProcessor.java          # Build steps
  deployment/src/main/resources/dev-ui/
    qwc-myfeature-dashboard.js            # Lit web components
  runtime-dev/src/main/java/.../runtime/dev/ui/
    MyFeatureJsonRpcService.java          # JSON-RPC service
```

- **JS naming:** `qwc-<extensionname>-<pagename>.js`
- **JSON-RPC services go in `runtime-dev/`**, not `runtime/`. Register as a
  conditional dev dependency — see the `classloading-and-runtime-dev` skill.

## Deployment Processor

Gate all Dev UI build steps with `@BuildStep(onlyIf = IsDevelopment.class)` or
use `@BuildSteps(onlyIf = IsLocalDevelopment.class)` at the class level.

```java
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildStep(onlyIf = IsDevelopment.class)
CardPageBuildItem devUI() {
    CardPageBuildItem card = new CardPageBuildItem();

    card.addPage(Page.webComponentPageBuilder()
            .title("Dashboard")
            .componentLink("qwc-myfeature-dashboard.js")
            .icon("font-awesome-solid:robot"));

    // Build-time data — available in JS via: import { items } from 'build-time-data';
    card.addBuildTimeData("items", someList);

    return card;
}
```

Register the JSON-RPC provider in a separate build step. This does not need the
`IsDevelopment` guard because the build item is only consumed in dev mode:

```java
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

@BuildStep
JsonRPCProvidersBuildItem jsonRpcProvider() {
    return new JsonRPCProvidersBuildItem(MyFeatureJsonRpcService.class);
}
```

**Maven dependency** for the deployment module:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-devui-spi</artifactId>
</dependency>
```

## Runtime JSON-RPC Service

A plain class in `runtime-dev`. Every public method becomes a JSON-RPC endpoint
automatically — registration happens via `JsonRPCProvidersBuildItem`.

```java
public class MyFeatureJsonRpcService {
    @Inject
    SomeBean bean;

    public List<Item> getItems() { return bean.listAll(); }
    public boolean doAction(String id) { return bean.execute(id); }
}
```

- Use `@Inject` for CDI; `@PostConstruct` for initialization.
- Return JSON-serializable data, **not** HTML.
- For streaming, return `Multi<JsonObject>` (Smallrye Mutiny).

## Frontend Web Components

Components extend `QwcHotReloadElement` (not `LitElement` directly) and use
Vaadin Web Components for consistent styling.

```javascript
import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { items } from 'build-time-data';

export class QwcMyfeatureDashboard extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);
    static properties = { _items: { state: true } };

    constructor() {
        super();
        this._items = items;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this.jsonRpc.getItems().then(r => { this._items = r.result; });
    }

    render() {
        if (!this._items)
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        return html`<vaadin-grid .items="${this._items}" theme="row-stripes">
            <vaadin-grid-column path="name" header="Name"></vaadin-grid-column>
        </vaadin-grid>`;
    }
}
customElements.define('qwc-myfeature-dashboard', QwcMyfeatureDashboard);
```

- `build-time-data` keys must match what was passed to `card.addBuildTimeData(key, value)`.
- `JsonRpc` method names must match the Java service method names exactly.
- Access results via `response.result`.
- For state updates, use spread: `this._items = [...this._items, newItem]`.
- Unsubscribe streaming observers in `disconnectedCallback()`.

## Testing

Extend `DevUIJsonRPCTest` (`io.quarkus.devui.tests`). Pass the extension
namespace to the super constructor, then call `executeJsonRPCMethod()`:

```java
public class MyFeatureDevUITest extends DevUIJsonRPCTest {
    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(MyBean.class));

    public MyFeatureDevUITest() { super("quarkus-myfeature"); }

    @Test
    public void testGetItems() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("getItems");
        assertNotNull(result);
    }
}
```

## Key Rules

- **Correct imports:** `CardPageBuildItem` is in `io.quarkus.devui.spi.page`,
  `JsonRPCProvidersBuildItem` is in `io.quarkus.devui.spi`.
- **JSON-RPC services belong in `runtime-dev/`**, never in `runtime/`.
- **JS files go in `deployment/src/main/resources/dev-ui/`**.
- **Extend `QwcHotReloadElement`**, not `LitElement` — it provides the
  `hotReload()` hook that re-runs on dev-mode restarts.
- **Return JSON from services**, not HTML. Use `Page.externalPageBuilder()` for
  external content like Swagger UI.