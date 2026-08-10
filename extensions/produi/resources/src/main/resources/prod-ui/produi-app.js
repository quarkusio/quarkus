// Prod UI entry point - bundled by esbuild at build time
// Register the Font Awesome iconset so <vaadin-icon icon="font-awesome-solid:..."> paints on the cards.
// The source files are copied from the quarkus-devui-resources jar into the bundle work dir at build time.
import './icon/font-awesome.js';
import './pui/pui-header.js';
import './pui/pui-empty-state.js';
import './pui/pui-extensions.js';
import './pui/pui-page-host.js';
import './pui/pui-advisor.js';
import './pui/pui-configuration.js';
import './pui/pui-endpoints.js';
import './pui/pui-loggers.js';
import './pui/pui-dependencies.js';
import './pui/pui-diagnostics.js';
import './pui/pui-app.js';
