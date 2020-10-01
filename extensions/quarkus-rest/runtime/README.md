** NOT IMPLEMENTED

- Lots of methods in the JAX-RS types
    - ~~HttpHeaders~~
    - ~~RuntimeDelegate~~
    - ~~Response~~
    - ~~ContainerRequestContext~~
    - ~~ContainerResponseContext~~
- Proper handling of generics
    - In entity return type
    - For reader/writer selection
- Async return types (single/stream)
    - Preliminary support for CS/Uni, but hacked in and not pluggable
- SSE
- Async request/response interceptors
- Custom reader/writer
    - Some support for this
    - No content type matching yet
    - Writers should add ContentType headers
- Reader/Writer interceptors
- Async Reader/Writer and interceptors
- Feature/DynamicFeature
- Header delegates
- Content negotiation
- Annotation inheritance
- Params
    - Matrix params (Stef votes this can wait)
    - RESTEasy variants (how?)
- Context
    - UriInfo
    - Application
    - Request
    - SecurityContext
    - Providers
    - ResourceContext
    - Configuration
    - Servlet replacements (RESTEasy has one, for remote IP)
    - Vertx context?
- BeanValidation
- Default readers/writers
    - Jsonb/Jsonp
    - Vertx JSON types
    - Async variants of spec
    - Spec:
        - byte[] All media types (*/*).
        - java.lang.String All media types (*/*).
        - java.io.InputStream All media types (*/*).
        - java.io.Reader All media types (*/*).
        - java.io.File All media types (*/*).
        - javax.activation.DataSource All media types (*/*).
        - javax.xml.transform.Source XML types (text/xml, application/xml and media types of the
        - form application/*+xml).
        - javax.xml.bind.JAXBElement and application-supplied JAXB classes XML types (text/xml and
        - application/xml and media types of the form application/*+xml).
        - MultivaluedMap<String,String> Form content (application/x-www-form-urlencoded).
        - StreamingOutput All media types (*/*), MessageBodyWriter only.
        - java.lang.Boolean, java.lang.Character, java.lang.Number Only

** JAXRS SPEC observations

*** Will not implement

- `ManagedBean`
- `DataSource`
- `Source`
- `JAXB`

*** Spec inconsistencies

- `ResponseBuilder.location(URI)` doc says relativise to request, but TCK tests relative to base
- `ResponseBuilder.location(URI)` doc says relativise using `UriInfo` but not available for client API
- `Response.readEntity` says entity stream should be closed, but TCK checks that the `Response` is closed instead
- `Response.readEntity` says entity can be retrieved by call to `getEntity()` but TCK closes the `Response`, which forbids calls to `getEntity()`
- `Response.getEntity` does not mention that the response being closed forbids getting an already read entity, but TCK checks that
- It's crazy that if there's a client `RequestFilter` that calls `abortWith(Response)`, we have to serialise the entity to run the response filter/interceptors
