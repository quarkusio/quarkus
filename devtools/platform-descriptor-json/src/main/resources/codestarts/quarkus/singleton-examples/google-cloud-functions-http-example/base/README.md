# Google Cloud Functions HTTP Integration examples

Examples of Google Cloud HTTP functions for Quarkus written with RESTEasy (JAX-RS), Undertow (Servlet), Vert.x Web, or Funqy HTTP.

Guide: https://quarkus.io/guides/gcp-functions-http

Inside the `src/main/java/org/acme/googlecloudfunctionshttp` directory, you will find examples for:

- JAX-RS (via RESTEasy): `GreetingResource.java`
- Vert.x reactive routes: `GreetingRoutes.java`
- Funqy HTTP: `GreetingFunqy`
- Servlet (via Undertow): `GreetingServlet.java`

Each of these example uses a different extension.
If you don't plan to use all those extensions, you should remove them from the `pom.xml`.

> :warning: **INCOMPATIBLE WITH DEV MODE**: Google Cloud Functions HTTP is not compatible with dev mode yet!
