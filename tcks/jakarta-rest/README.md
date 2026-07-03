# Jakarta REST TCK 4.0.1 Runner for Quarkus REST

This module runs the official [Jakarta REST TCK 4.0.1](https://github.com/jakartaee/rest/tree/4.0.1/jaxrs-tck)
against Quarkus REST using `quarkus-arquillian`.

## Running

The TCK is not part of the default build. Activate it with:

```shell
mvn verify -pl tcks/jakarta-rest -Drun-jakarta-rest-tck
```

## Architecture

- **quarkus-arquillian** restarts Quarkus for every test class via Arquillian's
  `deploy()`/`undeploy()` lifecycle.
- **Ephemeral ports** (`quarkus.http.test-port=0`) avoid port conflicts between
  test classes. The actual port is read from `ProtocolMetaData` after deploy and
  set as `System.setProperty("webServerPort", ...)`.
- **JUnit 5 `ExecutionCondition`** (`QuarkusRestTckDisabledTests`) disables tests
  that were already excluded in the old forked TCK runner (resteasy-reactive-testsuite).
  The `DisableReason` enum mirrors the old `QuarkusRest.java` classification.

## Current status (2026-07-03)

| Metric   | Count |
|----------|-------|
| Total    | 2755  |
| Pass     | 2338  |
| Error    | 162   |
| Failure  | 6     |
| Skipped  | 249   |
| Time     | ~6 min |

**249 skipped** = old TCK exclusions carried forward via `ExecutionCondition` +
`@Tag("se_bootstrap")`/`@Tag("servlet")` excluded via `excludedGroups` + signature test.

**168 new failures** not present in the old TCK are listed below. These need to be
investigated and either fixed in Quarkus REST or disabled with justification.

## New failures to investigate

### Sub-resource locator (128 tests)

All `*.sub.JAXRSSubClientIT` classes fail with sub-resource locator errors.
The old TCK only disabled specific methods in `formparam.sub` and `pathparam.sub`;
the remaining sub classes (`cookieparam.sub`, `headerparam.sub`, `matrixparam.sub`,
`queryparam.sub`) were not in the old TCK exclusion list.

These likely share the same root cause as the `*.locator.*` classes (which ARE
disabled), but need verification before disabling.

| Class | Errors |
|-------|--------|
| `ee.rs.cookieparam.sub.JAXRSSubClientIT` | 16 errors + 1 failure |
| `ee.rs.formparam.sub.JAXRSSubClientIT` | 21 errors (1 method disabled from old TCK) |
| `ee.rs.headerparam.sub.JAXRSSubClientIT` | 25 errors |
| `ee.rs.matrixparam.sub.JAXRSSubClientIT` | 26 errors |
| `ee.rs.pathparam.sub.JAXRSSubClientIT` | 13 errors (9 methods disabled from old TCK) |
| `ee.rs.queryparam.sub.JAXRSSubClientIT` | 27 errors |

### Cookie param entity conversion (12 tests)

`ee.rs.cookieparam.JAXRSClientIT` — 12 methods fail on cookie parameter entity
conversion (`cookieParamEntityWith*`, `cookieFieldParamEntityWith*`). The TCK sends
a cookie value and expects it to be converted via `fromString`/constructor/`valueOf`,
but Quarkus REST does not perform this conversion.

### Jakarta REST 4.0 new APIs (11 tests)

These test new API methods added in Jakarta REST 4.0 that are not yet implemented
in Quarkus REST:

- **`containsHeaderString(String, Predicate)`** (4 tests)
  - `api.client.clientrequestcontext.JAXRSClientIT#containsHeaderStringTest`
  - `api.client.clientresponsecontext.JAXRSClientIT#containsHeaderStringTest`
  - `ee.rs.container.responsecontext.JAXRSClientIT#containsHeaderStringTest`
  - `ee.rs.core.headers.JAXRSClientIT#containsHeaderStringTest`

- **`getMatchedResourceTemplate()`** (4 tests)
  - `jaxrs40.ee.rs.core.uriinfo.UriInfo40ClientIT#getMatchedResourceTemplateOneTest`
  - `jaxrs40.ee.rs.core.uriinfo.UriInfo40ClientIT#getMatchedResourceTemplateSubTest`
  - `jaxrs40.ee.rs.core.uriinfo.UriInfo40ClientIT#getMatchedResourceTemplateTwoGetTest`
  - `jaxrs40.ee.rs.core.uriinfo.UriInfo40ClientIT#getMatchedResourceTemplateTwoPostTest`

- **`getHeaderString()` semantics** (3 tests)
  - `api.client.clientresponsecontext.JAXRSClientIT#getHeaderStringIsEmptyTest`
  - `ee.rs.core.headers.JAXRSClientIT#getHeaderStringUsesToStringTest`
  - `ee.rs.core.headers.JAXRSClientIT#contentLanguageTest`

### Provider visibility (4 tests)

`spec.provider.visibility.JAXRSClientIT` — all 4 tests fail. Tests provider
visibility across applications (bodyWriter, bodyReader, contextResolver, exceptionMapper).

### Produces/Consumes media type matching (3 tests)

`ee.rs.produceconsume.JAXRSClientIT` — 3 methods fail on media type matching
with wildcards and XML types:
- `widgetsXmlAnyTest`
- `anyWidgetsxmlTest`
- `plainPlusProduceXmlTest`

### SSE timing (2 tests)

`jaxrs21.ee.sse.sseeventsource.JAXRSClientIT`:
- `wait2Seconds`
- `defaultWaiting1s`

These may be flaky/timing-sensitive rather than real bugs.

### Feature/DynamicFeature registration (2 tests)

`jaxrs31.spec.extensions.JAXRSClientIT`:
- `featureIsRegisteredTest`
- `dynamicFeatureIsRegisteredTest`

Jakarta REST 3.1 feature registration — features may not be getting registered
at the right lifecycle point.

### Build failures (2 tests)

These classes fail during Quarkus augmentation (build time):
- `jaxrs31.ee.multipart.MultipartSupportIT` — multipart support issue
- `spec.contextprovider.JsonbContextProviderIT` — JSON-B context provider issue

### Security context (1 test)

`ee.rs.core.securitycontext.basic.JAXRSBasicClientIT#noAuthorizationTest` — test
sends a request without credentials and expects a specific security context state.

### Client exception (1 test)

`spec.client.exceptions.ClientExceptionsIT#shouldThrowMostSpecificWebApplicationException`

### Resource constructor (1 test)

`spec.resourceconstructor.JAXRSClientIT#visibleTest` — resource constructor
selection/visibility issue.

## Files

| File | Purpose |
|------|---------|
| `pom.xml` | TCK dependency, failsafe configuration, profile activation |
| `QuarkusRestTckArchiveProcessor.java` | Arquillian observer: injects `application.properties`, reads ephemeral port |
| `QuarkusRestTckDisabledTests.java` | JUnit 5 `ExecutionCondition` — disables old TCK exclusions |
| `DisableReason.java` | Enum of exclusion reason categories (mirrors old `QuarkusRest.java`) |
| `META-INF/services/org.junit.jupiter.api.extension.Extension` | Service loader for auto-detection |
| `junit-platform.properties` | Enables JUnit 5 extension auto-detection |
| `arquillian.xml` | Arquillian configuration for Quarkus |
