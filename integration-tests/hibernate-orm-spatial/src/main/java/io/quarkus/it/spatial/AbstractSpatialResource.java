package io.quarkus.it.spatial;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public abstract class AbstractSpatialResource {
    @Inject
    EntityManager em;

    protected abstract Object getPolygon();

    protected abstract String getPointEntityName();

    protected abstract String getLineEntityName();

    protected abstract String getPolygonEntityName();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/point/{id}")
    public Boolean jtsPoint(@PathParam("id") final Long id) {
        return em
                .createQuery(String.format("select st_within(e.point, :geom) from %s e where e.id = :id", getPointEntityName()),
                        Boolean.class)
                .setParameter("geom", getPolygon())
                .setParameter("id", id)
                .getSingleResult();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/line/{id}")
    public String jtsLine(@PathParam("id") final Long id) {
        return em
                .createQuery(String.format("select st_astext(e.line) from %s e where e.id = :id", getLineEntityName()),
                        String.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/polygon/{id}")
    public Integer jtsPolygon(@PathParam("id") final Long id) {
        return em
                .createQuery(String.format("select st_srid(e.polygon) from %s e where e.id = :id",
                        getPolygonEntityName()), Integer.class)
                .setParameter("id", id)
                .getSingleResult();
    }
}
