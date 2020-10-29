** NOT IMPLEMENTED

- Proper handling of generics (not clear if we do, in fact)
    - In entity return/parameter type
    - For reader/writer selection
    - For param converter selection
- Async return types (single/stream)
    - Preliminary support for CS/Uni, but hacked in and not pluggable
- SSE
    - Reconnect special responses that should stop reconnecting
    - Last-Event-Id not sent
- RESTEasy extensions
    - Async Reader/Writer and interceptors: do we need this?
    - Servlet replacement for remote host/IP (HttpRequest.getRemoteHost/Address)
    - resteasy-links (used by quarkus-data-rest)
- Default readers/writers
    - Vertx JSON types
    - Async variants of spec
    - Spec:
        - javax.activation.DataSource All media types (*/*).
        - javax.xml.transform.Source XML types (text/xml, application/xml and media types of the
          form application/*+xml).
        - javax.xml.bind.JAXBElement and application-supplied JAXB classes XML types (text/xml and
          application/xml and media types of the form application/*+xml).
- Callbacks on async responses
    - CompletionCallback supported
    - ConnectionCallback not supported yet
- We use the request context to store and inject provider types, which means they are also injected
  on client resources if the client runs on the server, and just not injected if the client is not
  on the server. This is wrong.

** TODO later list

- XML?
- Optim: CookieParser splits things without checking if a separator exists, but it's too weird to touch ATM (see spec discussion)
- I don't think we handle generic endpoints well: we don't appear to apply type arguments for methods defined in generic supertypes,
  so we see all method parameters as `Object`. Same for bean params.
- SSE questions
  - Should we add @Sse annotation instead of @Produces(MediaType.SERVER_SENT_EVENTS), and use @Produces for the current @SseElementType?
  - Should we do something to prevent reconnect when the server is done?

** JAXRS SPEC observations

*** Will not implement

- `ManagedBean`
- `DataSource`
- `Source`
- `StreamingOutput`
- `JAXB`

*** Spec inconsistencies

- `ResponseBuilder.location(URI)` doc says relativise to request, but TCK tests relative to base
- `ResponseBuilder.location(URI)` doc says relativise using `UriInfo` but not available for client API
- `Response.readEntity` says entity stream should be closed, but TCK checks that the `Response` is closed instead
- `Response.readEntity` says entity can be retrieved by call to `getEntity()` but TCK closes the `Response`, which forbids calls to `getEntity()`
- `Response.getEntity` does not mention that the response being closed forbids getting an already read entity, but TCK checks that
- It's crazy that if there's a client `RequestFilter` that calls `abortWith(Response)`, we have to serialise the entity to run the response filter/interceptors
- `AbstractMultivaluedMap.putAll(MultivaluedMap)` will add the parameter's `List` values without copying, directly to the store, which means that 
  further calls to `addAll()` will modify those lists, effectively having both maps share their mutable `List` storage. 
- `MultivaluedMap` is missing `addAll(MultivaluedMap)` to complete `putAll`
- The TCK in JAXRSClient0162 tests sending pre-serialised JSON strings as JSON, and so the JSON serialiser had to be modified to not serialise String
  entities, but that's wrong, because it will properly serialise Boolean, Number and any Object type. Also if you deserialise a JSON string value `"foo"`
  you will get `foo` without the quotes, whereas with this fix, if we now send a `foo` String value as JSON it will be sent raw (assumed pre-serialised)
  so this is not regular. We should always serialise string values as JSON string values, and introduce a `SerialisedJsonString` type to mark pre-serialised
  JSON strings. Possibly even just `RawString` to make sure no serialise will modify them.
- Request Cookies are based on https://tools.ietf.org/html/rfc2109 which have the clients send cookie params to the server, using `$`-prefixed parameter names,
  but it was obsoleted in https://tools.ietf.org/html/rfc2965 and then in https://tools.ietf.org/html/rfc6265, which does not send cookie params to the server
  and those params are not `$`-prefixed anymore.
- Should add HTTP status codes constants, without requiring to go via Response.Status.OK.getStatus() which is not a constant
- Parameter-less `@*Param` annotations, make `@Context` optional for method parameters, same for `@PathParam`
- Turn `@BeanParam` into a type declaration
- Support the new annotated filters
- Spec should say when `CompletionCallback` run: is it after the request is sent, or before? ATM it can be before (that's how we do it
  but it leads in tests that get a response before the callbacks are run and a nightmare to test because the next request can be executed
  before the callbacks are run).
- We should fix the client `WebApplicationException` usage that leaks the `Response` when on the server

*** Spec extensions proposed

- We should disfavour ParamConverterProvider and allow `@Context` on `ParamConverter` since it has a type param, we could scan those at build time
  rather than do runtime-resolving.

*** Not tested by the TCK (and not implemented)

- Parameter converters that throw exceptions should produce NOT_FOUND or BAD_REQUEST, and we implemented it for method params, but
  not for beanparams/resource fields.
- SSE client should reconnect with Last-Event-Id.
- SSE client should handle certain HTTP status codes from the server.
- Connection Callbacks on AsyncResponse
