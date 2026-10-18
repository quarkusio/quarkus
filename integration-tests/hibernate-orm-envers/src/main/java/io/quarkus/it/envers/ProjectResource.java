package io.quarkus.it.envers;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.StartupEvent;

@Path("/project")
public class ProjectResource {

    @Inject
    EntityManager em;

    @Transactional
    public void startup(@Observes final StartupEvent startupEvent) {
        final Project project = new Project();
        project.setName("Quarkus");
        em.persist(project);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/{id}")
    public String getProjectAtLastRevision(@PathParam("id") final Long id) {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("On IO Thread");
        }
        final Project projectAudited = (Project) AuditReaderFactory.get(em)
                .createQuery()
                .forRevisionsOfEntity(
                        Project.class, true, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getSingleResult();
        return projectAudited.getName();
    }

}
