package org.acme

import org.acme.api.AdminAPI
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo


@ApplicationScoped
class AdminResource : AdminAPI {

  override suspend fun createAccount(tenantId: String, body: String, uriInfo: UriInfo): Response {
    return Response.noContent().build()
  }

}
