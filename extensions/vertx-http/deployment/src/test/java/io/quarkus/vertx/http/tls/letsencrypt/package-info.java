/**
 * This package contains tests for the let's encrypt routes defined in the TLS registry.
 * Because of the Vert.x HTTP -> TLS Registry dependency, these tests are in the Vert.x HTTP module, even if the
 * routes are implemented in the TLS Registry extension.
 */
package io.quarkus.vertx.http.tls.letsencrypt;