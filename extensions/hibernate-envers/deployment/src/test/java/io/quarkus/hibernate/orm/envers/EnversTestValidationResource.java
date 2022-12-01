package io.quarkus.hibernate.orm.envers;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.resource.spi.IllegalStateException;
import javax.transaction.UserTransaction;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
