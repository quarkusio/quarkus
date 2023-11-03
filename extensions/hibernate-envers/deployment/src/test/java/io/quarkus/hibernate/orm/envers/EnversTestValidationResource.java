package io.quarkus.hibernate.orm.envers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.resource.spi.IllegalStateException;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

@Path("/envers")
@ApplicationScoped
public class EnversTestValidationResource {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @POST
    public String save(String name) {
        try {
            transaction.begin();
            MyAuditedEntity entity = new MyAuditedEntity();
            entity.setName("initial");
            em.persist(entity);
            transaction.commit();

            transaction.begin();
            entity.setName(name);
            em.merge(entity);
            em.flush();
            transaction.commit();

            AuditReader auditReader = AuditReaderFactory.get(em);
            List<Number> revisions = auditReader.getRevisions(MyAuditedEntity.class, entity.getId());
            if (revisions.size() != 2) {
                throw new IllegalStateException(String.format("found {} revisions", revisions.size()));
            }

            MyRevisionEntity revEntity = auditReader.findRevision(MyRevisionEntity.class, revisions.get(0));
            if (revEntity.getListenerValue() == null) {
                throw new IllegalStateException("revision listener failed to update revision entity");
            }

            return "OK";
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }
}
