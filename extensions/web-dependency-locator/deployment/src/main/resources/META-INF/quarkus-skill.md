### Adding Web Dependencies

Use **mvnpm** (preferred) or **WebJars** to add JavaScript/CSS libraries as Maven dependencies:

```xml
<!-- mvnpm (npm packages as Maven artifacts) -->
<dependency>
    <groupId>org.mvnpm</groupId>
    <artifactId>bootstrap</artifactId>
    <version>${bootstrap.version}</version>
</dependency>

<!-- WebJars -->
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>jquery-ui</artifactId>
    <version>${jquery.version}</version>
</dependency>
```

mvnpm packages are served at `/_static/{name}/` and WebJars at `/webjars/{name}/`. Version numbers are **not needed** in URLs — the extension handles version rerouting automatically.

### Import Maps

The extension auto-generates ES6 import maps from mvnpm dependencies, enabling clean JavaScript imports:

```javascript
// Instead of: import '/_static/bootstrap/5.3.3/dist/js/bootstrap.min.js'
import 'bootstrap/dist/js/bootstrap.min.js';
```

Add custom import mappings in `application.properties`:

```properties
quarkus.web-dependency-locator.import-mappings.app/=/_static/app/
```

### Static Web Assets

Place web files in `src/main/resources/web/`:

```
src/main/resources/web/
├── index.html
└── app/
    ├── style.css
    └── main.js
```

Use HTML directives for automatic injection:
- `#importmap` — injects the import map script tag
- `#bundle` — injects all CSS links, import map, and JS module imports from `app/`

```html
<!DOCTYPE html>
<html>
<head>
    #bundle
</head>
<body>
    <h1>My App</h1>
</body>
</html>
```

### Testing

- Use `@QuarkusTest` — static assets are served in test mode.
- Use RestAssured to verify asset serving: `given().when().get("/_static/bootstrap/dist/js/bootstrap.min.js").then().statusCode(200)`
- Test import map generation: `GET /_importmap/generated_importmap.js`

### Common Pitfalls

- Do NOT include version numbers in URLs — the extension handles version rerouting. Use `/webjars/jquery-ui/jquery-ui.min.js` not `/webjars/jquery-ui/1.14.1/jquery-ui.min.js`.
- For assets using `#importmap` or `#bundle` directives, place them in `src/main/resources/web/`. Standard static assets can still use `META-INF/resources/`.
- Import maps are only generated for **mvnpm** dependencies, not for WebJars.
- The `#bundle` directive only discovers CSS/JS files under the `app/` subdirectory (configurable via `quarkus.web-dependency-locator.app-root`).
- HTML files with `#importmap` or `#bundle` are processed at **build time** — changes require a rebuild (hot reload handles this in dev mode).
