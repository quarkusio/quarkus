package org.acme.api

import javax.validation.Valid
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path(value = "/admin/api/v1.0")
@Produces(value = ["application/json"])
@Consumes(value = ["application/json"])
interface AdminAPI {

  @POST
  @Path(value = "/tenants/{tenantId}/accounts")
  suspend fun createAccount(
    @PathParam(value = "tenantId") tenantId: String,
    body: String,
    @Context uriInfo: UriInfo
  ): Response

}
