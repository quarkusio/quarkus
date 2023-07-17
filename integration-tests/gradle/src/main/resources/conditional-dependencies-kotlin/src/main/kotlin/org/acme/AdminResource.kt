package org.acme

import org.acme.api.AdminAPI
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo


@ApplicationScoped
class AdminResource : AdminAPI {

  override suspend fun createAccount(tenantId: String, body: String, uriInfo: UriInfo): Response {
    return Response.noContent().build()
  }

}
