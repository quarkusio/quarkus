package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.attributeconverter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/xml-mapping-only/attribute-converter")
@ApplicationScoped
public class XmlMappingOnlyAttributeConverterResource {

    @Inject
    @PersistenceUnit("xml-mapping-only-attribute-converter")
    Session session;

    @Inject
    UserTransaction transaction;

    @GET
    @Path("/auto-apply/")
    @Produces(MediaType.TEXT_PLAIN)
    public String autoApply() throws Exception {
        try {
            transaction.begin();
            MyEntity entity = new MyEntity();
            entity.myData = new MyData("foo");
            session.persist(entity);
            transaction.commit();

            transaction.begin();
            assertThat(session.createNativeQuery("select myData from myentity").getResultList())
                    .containsExactly("foo");
            transaction.commit();
        } catch (Exception | AssertionError e) {
            try {
                transaction.rollback();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }

        return "OK";
    }
}
