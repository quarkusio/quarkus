### Alternatives

- For more advanced frontend needs, consider the Quarkiverse **Web Bundler** extension (`quarkus-web-bundler`) — it provides bundling, transpiling, and a more integrated frontend development experience.
- For projects with a full Node.js-based frontend (React, Angular, Vue, etc.), consider the Quarkiverse **Quinoa** extension (`quarkus-quinoa`) — it manages the Node.js build lifecycle and serves the frontend from Quarkus.

### WebJars and mvnpm

- Add web dependencies (WebJars or mvnpm) as Maven/Gradle dependencies — the extension serves them automatically.
- Reference assets **without version numbers**: `/webjars/jquery/jquery.min.js` instead of `/webjars/jquery/3.1.1/jquery.min.js`.
- mvnpm assets are served under `/_static/` without version paths.

### ImportMap Generation

- The extension auto-generates an importmap at `/_importmap/generated_importmap.js`.
- Use ES module imports directly: `import { LitElement, html, css } from 'lit';` — no manual version management needed.
- Add custom import mappings with `quarkus.web-dependency-locator.import-mappings.<key>=<path>`.

### Bundle Tag

- Place web assets in `src/main/resources/web` (not `META-INF/resources`) to use the `{#bundle /}` tag.
- The `{#bundle /}` tag auto-generates importmap scripts and includes CSS/JS from the `/app` directory.
- Supports hot-reload in dev mode — no manual HTML updates when adding libraries.

### Testing

- Use `@QuarkusTest` with REST Assured to verify static resources are served.
- Assert that `/webjars/<library>/<file>` returns HTTP 200 and expected content.
- Verify importmap generation by fetching `/_importmap/generated_importmap.js`.

### Common Pitfalls

- Do NOT include version numbers in resource paths — the extension resolves them automatically.
- Do NOT place assets in `META-INF/resources` if you want to use the `{#bundle /}` tag — use `src/main/resources/web` instead.
- Ensure the web dependency JAR is on the classpath — the extension only serves resources from declared dependencies.
