---
name: writing-extension-devui
description: >
  How to create a Quarkus Dev UI extension with build-time pages, runtime JSON-RPC services, and Lit web components. Use this skill whenever the user wants to add a Dev UI page, create a Dev UI card, build a Dev UI extension, add developer tooling to a Quarkus extension, expose runtime data in Quarkus Dev UI, create a JSON-RPC service for Dev UI, or build any interactive developer dashboard inside a Quarkus application. Also use when the user mentions CardPageBuildItem, JsonRPCProvidersBuildItem, qwc- web components, or dev-ui directories in a Quarkus extension context.
---

# Writing a Dev UI for a Quarkus Extension

Quarkus Dev UI is an interactive developer dashboard available at `/q/dev-ui` during `quarkus:dev`. Extensions add their own pages by producing build items at deployment time and providing runtime services + frontend components.

The architecture has three layers:

1. **Deployment processor** (build time) -- collects metadata, registers pages and JSON-RPC providers
2. **Runtime JSON-RPC service** -- serves live data to the frontend over WebSocket
3. **Lit web components** (frontend) -- render pages using Vaadin Web Components

---

## Directory Layout

A Quarkus extension that provides Dev UI follows this structure:

```
my-extension/
  deployment/
    src/main/java/.../deployment/devui/
      MyDevUIProcessor.java          # Build steps
      MyFeatureInfo.java             # DTOs for build-time data
    src/main/resources/dev-ui/
      qwc-my-feature.js              # Lit web components
  runtime/
    src/main/java/.../runtime/devui/
      MyJsonRpcService.java          # JSON-RPC endpoints
```

Frontend JS files go in `deployment/src/main/resources/dev-ui/`. The naming convention is `qwc-<feature>.js` (Quarkus Web Component).

---

## Layer 1: Deployment Processor

The processor registers Dev UI pages and injects build-time data. All Dev UI build steps must be gated with `@BuildStep(onlyIf = IsDevelopment.class)`.

### Registering pages

```java
import io.quarkus.deployment.dev.devui.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildStep(onlyIf = IsDevelopment.class)
CardPageBuildItem cardPage(/* inject build items for your extension */) {
    CardPageBuildItem card = new CardPageBuildItem();

    // Page backed by a web component
    card.addPage(Page.webComponentPageBuilder()
        .title("My Feature")
        .componentLink("qwc-my-feature.js")
        .icon("font-awesome-solid:robot")
        .staticLabel(String.valueOf(itemCount)));   // badge on the card

    // Multiple pages on one card
    card.addPage(Page.webComponentPageBuilder()
        .title("Details")
        .componentLink("qwc-my-details.js")
        .icon("font-awesome-solid:list"));

    return card;
}
```

### Injecting build-time data

Build-time data is serialized to JSON and made available to frontend components via a magic `build-time-data` import.

```java
@BuildStep(onlyIf = IsDevelopment.class)
CardPageBuildItem cardPage(List<DetectedFeatureBuildItem> features) {
    CardPageBuildItem card = new CardPageBuildItem();

    List<FeatureInfo> infos = features.stream()
        .map(f -> new FeatureInfo(f.getName(), f.getType()))
        .toList();
    card.addBuildTimeData("features", infos);

    card.addPage(Page.webComponentPageBuilder()
        .title("Features")
        .componentLink("qwc-features.js")
        .icon("font-awesome-solid:list"));

    return card;
}
```

The DTO class needs no annotations -- Jackson serializes it automatically via getters:

```java
public class FeatureInfo {
    private final String name;
    private final String type;

    public FeatureInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
}
```

### Registering JSON-RPC providers

A separate build step registers the runtime JSON-RPC service class. Public methods on the class automatically become JSON-RPC endpoints.

```java
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

@BuildStep
void jsonRpcProviders(BuildProducer<JsonRPCProvidersBuildItem> producers) {
    producers.produce(new JsonRPCProvidersBuildItem(MyJsonRpcService.class));
}
```

This build step does not need the `IsDevelopment` guard because the build item itself is only consumed in dev mode.

### Conditional registration

Register pages or providers only when required beans or configuration exist:

```java
@BuildStep
void jsonRpcProviders(
        BuildProducer<JsonRPCProvidersBuildItem> producers,
        List<SomeProviderCandidateBuildItem> candidates) {
    if (!candidates.isEmpty()) {
        producers.produce(new JsonRPCProvidersBuildItem(MyJsonRpcService.class));
    }
}
```

### Allowing other modules to contribute pages

When multiple modules should contribute pages to the same card, define a `MultiBuildItem`:

```java
public final class AdditionalPageBuildItem extends MultiBuildItem {
    private final String title;
    private final String icon;
    private final String componentLink;
    private final Map<String, Object> buildTimeData;

    // constructor, getters
}
```

In the main processor, collect these and add them to the card:

```java
@BuildStep(onlyIf = IsDevelopment.class)
CardPageBuildItem cardPage(List<AdditionalPageBuildItem> additionalPages) {
    CardPageBuildItem card = new CardPageBuildItem();
    for (AdditionalPageBuildItem extra : additionalPages) {
        card.addPage(Page.webComponentPageBuilder()
            .title(extra.getTitle())
            .componentLink(extra.getComponentLink())
            .icon(extra.getIcon()));
        extra.getBuildTimeData().forEach(card::addBuildTimeData);
    }
    return card;
}
```

### Maven dependency

The deployment module needs the Dev UI SPI:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-devui-spi</artifactId>
</dependency>
```

---

## Layer 2: Runtime JSON-RPC Service

A JSON-RPC service is a plain CDI bean. Every public method is automatically exposed as a JSON-RPC endpoint. No annotations are needed on the class itself -- registration happens via the `JsonRPCProvidersBuildItem` in the processor.

### Basic service

```java
public class MyJsonRpcService {

    @Inject
    SomeCdiBean myBean;

    // Synchronous endpoint: jsonRpc.getData({id: "123"})
    public MyResult getData(String id) {
        return myBean.find(id);
    }

    // Boolean endpoint
    public boolean isFeatureEnabled() {
        return myBean.isEnabled();
    }
}
```

### Constructor injection

For complex dependency resolution (multiple beans, qualifier lookup):

```java
public class MyJsonRpcService {

    private final MyModel model;

    public MyJsonRpcService(@All List<MyModel> models) {
        // Pick the default-qualified bean, or fall back to first
        this.model = models.stream()
            .filter(m -> /* check qualifiers */)
            .findFirst()
            .orElse(models.get(0));
    }
}
```

Use `Arc.container().select(Class, qualifier).get()` for programmatic CDI lookups when needed.

### Streaming responses

Return `Multi<JsonObject>` (Smallrye Mutiny reactive type) for streaming data to the frontend. The frontend receives each emitted item as a separate JSON-RPC notification.

```java
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.JsonObject;

public class MyJsonRpcService {

    // Streaming endpoint: jsonRpc.streamUpdates().onNext(...)
    public Multi<JsonObject> streamUpdates() {
        return Multi.createFrom().emitter(emitter -> {
            try {
                // Emit individual items
                emitter.emit(new JsonObject().put("status", "processing"));
                emitter.emit(new JsonObject().put("progress", 50));
                emitter.emit(new JsonObject().put("status", "done"));
                emitter.complete();
            } catch (Exception e) {
                emitter.fail(e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
```

Use semantic keys in the emitted `JsonObject` so the frontend can distinguish message types:

```java
// Token streaming
emitter.emit(new JsonObject().put("token", partialText));

// Tool execution event
emitter.emit(new JsonObject().put("toolRequest", toolJson));

// Final result
emitter.emit(new JsonObject().put("message", finalResult));
```

### Request context

If the service needs request-scoped CDI beans, annotate methods with `@ActivateRequestContext`:

```java
import jakarta.enterprise.context.control.ActivateRequestContext;

@ActivateRequestContext
public MyResult processWithContext(String input) {
    // request-scoped beans are available here
}
```

### State management

For services that maintain conversation or session state, hold state in instance fields. The service is instantiated once per Dev UI session.

```java
public class MyChatService {

    private ChatMemory memory;

    public String reset(String systemMessage) {
        memory = new InMemoryChatMemory();
        memory.add(SystemMessage.from(systemMessage));
        return "OK";
    }

    public MyResult chat(String message) {
        memory.add(UserMessage.from(message));
        // ... process and return
    }
}
```

### Error handling

Return error information as part of the response object rather than throwing exceptions (which surface as generic JSON-RPC errors):

```java
public MyResultPojo doSomething(String input) {
    try {
        Object result = process(input);
        return new MyResultPojo(result, null);
    } catch (Exception e) {
        return new MyResultPojo(null, e.getMessage());
    }
}
```

---

## Layer 3: Frontend Lit Web Components

Frontend components use the Lit framework with Vaadin Web Components. They communicate with the backend via the Quarkus JSON-RPC client.

### Basic component with build-time data

```javascript
import { LitElement, html, css } from 'lit';
import { features } from 'build-time-data';

export class QwcFeatures extends LitElement {

    static styles = css`
        .container { padding: 1rem; }
        vaadin-grid { height: 100%; }
    `;

    static properties = {
        _features: { state: true },
        _filter: { state: true },
    };

    constructor() {
        super();
        this._features = features;  // from build-time-data
        this._filter = '';
    }

    render() {
        const filtered = this._features.filter(f =>
            f.name.toLowerCase().includes(this._filter.toLowerCase())
        );

        return html`
            <div class="container">
                <vaadin-text-field
                    placeholder="Filter..."
                    @input="${e => this._filter = e.target.value}">
                </vaadin-text-field>

                <vaadin-grid .items="${filtered}" theme="row-stripes">
                    <vaadin-grid-column path="name" header="Name"></vaadin-grid-column>
                    <vaadin-grid-column path="type" header="Type"></vaadin-grid-column>
                </vaadin-grid>
            </div>
        `;
    }
}

customElements.define('qwc-features', QwcFeatures);
```

Key points:
- Import build-time data via `import { key } from 'build-time-data'` -- the key name must match what was passed to `card.addBuildTimeData(key, value)`.
- Use `static properties` with `{state: true}` for reactive internal state (triggers re-render on change).
- Assign build-time data in the constructor.
- Use Vaadin Web Components (`vaadin-grid`, `vaadin-text-field`, `vaadin-button`, `vaadin-select`, etc.) for consistent styling.

### Component with JSON-RPC calls

```javascript
import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';

export class QwcMyDashboard extends LitElement {

    static properties = {
        _data: { state: true },
        _loading: { state: true },
        _error: { state: true },
    };

    jsonRpc = new JsonRpc(this);

    constructor() {
        super();
        this._data = null;
        this._loading = true;
        this._error = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._loadData();
    }

    _loadData() {
        this._loading = true;
        this.jsonRpc.getData({ id: 'main' })
            .then(response => {
                this._data = response.result;
                this._loading = false;
            })
            .catch(error => {
                this._error = error;
                this._loading = false;
            });
    }

    render() {
        if (this._loading) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        if (this._error) {
            return html`<p style="color:red">${this._error}</p>`;
        }
        return html`
            <div>
                <vaadin-button @click="${() => this._loadData()}">Refresh</vaadin-button>
                <pre>${JSON.stringify(this._data, null, 2)}</pre>
            </div>
        `;
    }
}

customElements.define('qwc-my-dashboard', QwcMyDashboard);
```

Key points:
- Create `new JsonRpc(this)` as an instance field.
- Call methods with `this.jsonRpc.methodName({param1: value1, param2: value2})`.
- Parameter names and method names must match the Java service exactly.
- Access the result via `response.result`.

### Streaming responses

```javascript
connectedCallback() {
    super.connectedCallback();
    this._subscribe();
}

disconnectedCallback() {
    super.disconnectedCallback();
    if (this._observer) {
        this._observer.unsubscribe();
    }
}

_subscribe() {
    this._observer = this.jsonRpc.streamUpdates()
        .onNext(response => {
            if (response.result.token) {
                this._text += response.result.token;
                this.requestUpdate();
            } else if (response.result.status === 'done') {
                this._loading = false;
            }
        })
        .onError(error => {
            this._error = error;
            this._loading = false;
        });
}
```

Always unsubscribe in `disconnectedCallback()` to avoid memory leaks.

### State updates that trigger re-render

```javascript
// Direct assignment to a reactive property
this._items = [...this._items, newItem];     // spread for arrays
this._count = this._count + 1;               // primitives
this._selected = newSelection;               // object replacement

// Force re-render when mutating in place
this._items.push(newItem);
this.requestUpdate();
```

### Embedding external HTML (iframe)

When you have pre-generated HTML (e.g., from a report generator), embed it in an iframe:

```javascript
render() {
    if (!this._htmlContent) {
        return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
    }
    return html`
        <iframe
            .srcdoc="${this._htmlContent}"
            style="width:100%; height:100%; border:none;"
            sandbox="allow-scripts">
        </iframe>
    `;
}

connectedCallback() {
    super.connectedCallback();
    this.jsonRpc.getReportHtml()
        .then(response => { this._htmlContent = response.result; });
}
```

---

## Wiring Dev-Mode-Only Runtime Beans

Sometimes you need a runtime bean that only exists in dev mode (e.g., a monitor, a state holder). Use a `SyntheticBeanBuildItem`:

```java
@BuildStep(onlyIf = IsDevelopment.class)
SyntheticBeanBuildItem devModeHolder() {
    return SyntheticBeanBuildItem
        .configure(DevModeStateHolder.class)
        .scope(Singleton.class)
        .unremovable()
        .defaultBean()
        .setRuntimeInit()
        .supplier(recorder.createDevModeHolder())
        .done();
}
```

The recorder supplies the bean instance:

```java
@Recorder
public class MyRecorder {
    @RuntimeInit
    public Supplier<DevModeStateHolder> createDevModeHolder() {
        return DevModeStateHolder::new;
    }
}
```

To inject dev-mode-only behavior into existing agent/service creation without breaking production, use a static flag pattern:

```java
@Recorder
public class MyRecorder {
    private static volatile boolean devMonitoringEnabled = false;

    @RuntimeInit
    public void enableDevModeMonitoring() {
        devMonitoringEnabled = true;
    }

    // In your existing bean creation logic:
    // if (devMonitoringEnabled) { /* attach monitor, register with holder */ }
}
```

The deployment processor calls this from a dev-mode-only build step:

```java
@BuildStep(onlyIf = IsDevelopment.class)
@Record(ExecutionTime.RUNTIME_INIT)
void enableMonitoring(MyRecorder recorder) {
    recorder.enableDevModeMonitoring();
}
```

---

## Testing Dev UI Services

Use `DevUIJsonRPCTest` for integration testing of JSON-RPC services:

```java
public class MyDevUIJsonRpcTest extends DevUIJsonRPCTest {

    @Test
    public void testGetData() throws Exception {
        JsonNode result = executeJsonRPCMethod("getData",
            Map.of("id", "test-123"));
        assertNotNull(result);
        assertEquals("expected-value", result.get("name").asText());
    }

    @Test
    public void testAddAndRetrieve() throws Exception {
        String id = executeJsonRPCMethod(String.class, "add",
            Map.of("text", "Hello world", "metadata", "key=value"));
        assertNotNull(id);

        JsonNode results = executeJsonRPCMethod("findRelevant",
            Map.of("text", "Hello", "limit", "10"));
        assertTrue(results.isArray());
        assertTrue(results.size() > 0);
    }
}
```

---

## Checklist

When building a Dev UI extension, verify:

- [ ] Processor has `@BuildStep(onlyIf = IsDevelopment.class)` on card registration
- [ ] Build-time data keys match the `import { ... } from 'build-time-data'` names in JS
- [ ] JSON-RPC method names in JS match the Java public method names exactly
- [ ] Streaming endpoints return `Multi<JsonObject>` and use `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`
- [ ] Frontend components call `customElements.define('qwc-my-name', QwcMyName)` matching the `componentLink` in the processor
- [ ] Streaming subscriptions are cleaned up in `disconnectedCallback()`
- [ ] Dev-mode-only beans use `SyntheticBeanBuildItem` with `IsDevelopment` guard
- [ ] DTOs have getters (Jackson serializes via getters automatically)