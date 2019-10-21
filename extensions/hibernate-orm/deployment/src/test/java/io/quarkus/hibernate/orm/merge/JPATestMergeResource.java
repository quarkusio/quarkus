/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package io.quarkus.hibernate.orm.merge;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Andrea Boriero
 */
@Path("/testresource/")
@ApplicationScoped
@Transactional
public class JPATestMergeResource {
    @Inject
    EntityManager em;

    @GET
    @Path("/persist")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TestEntity persist(TestEntity testEntity) {
        em.persist(testEntity);
        return testEntity;
    }

    @POST
    @Path("/merge")
    @Consumes(MediaType.APPLICATION_JSON)
    public void merge(TestEntity testEntity) {
        em.merge(testEntity);
    }

    @POST
    @Path("/delete")
    public void deleteAll() {
        em.createQuery("from TestEntity").getResultList().forEach(
                entity -> {
                    em.remove(entity);
                });
    }

    @GET
    @Path("/find")
    @Produces(MediaType.APPLICATION_JSON)
    public TestEntity find(@QueryParam("id") Long id) {
        return em.find(TestEntity.class, id);
    }
}
