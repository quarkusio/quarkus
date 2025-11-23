package io.quarkus.it.hibernate.processor.data.security;

import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.it.hibernate.processor.data.puother.AuthenticatedMyOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.DenyAllMyOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.MyOtherEntity;
import io.quarkus.it.hibernate.processor.data.puother.ParentClassSecurityMyOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.ParentMethodSecurityMyOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.PermissionsAllowedMetaAnnotationOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.PermissionsAllowedOtherRepository;
import io.quarkus.it.hibernate.processor.data.puother.RolesAllowedOtherRepository;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/secured/data/other")
public class SecuredMyOtherEntityResource {

    @Inject
    AuthenticatedMyOtherRepository authenticatedMyOtherRepository;

    @Inject
    RolesAllowedOtherRepository rolesAllowedOtherRepository;

    @Inject
    PermissionsAllowedOtherRepository permissionsAllowedOtherRepository;

    @Inject
    PermissionsAllowedMetaAnnotationOtherRepository permissionsAllowedMetaAnnotationOtherRepository;

    @Inject
    DenyAllMyOtherRepository denyAllMyOtherRepository;

    @Inject
    ParentMethodSecurityMyOtherRepository parentMethodSecurityMyOtherRepository;

    @Inject
    ParentClassSecurityMyOtherRepository parentClassSecurityMyOtherRepository;

    @GET
    @Transactional
    @Path("/by/name/{name}/authenticated")
    public MyOtherEntity getByNameAuthenticated(@RestPath String name) {
        return findByName(name, authenticatedMyOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/roles-allowed")
    public MyOtherEntity getByNameRolesAllowed(@RestPath String name) {
        return findByName(name, rolesAllowedOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/permissions-allowed")
    public MyOtherEntity getByNamePermissionsAllowed(@RestPath String name) {
        return findByName(name, permissionsAllowedOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/permissions-allowed-meta-annotation")
    public MyOtherEntity getByNamePermissionsAllowedMetaAnnotation(@RestPath String name) {
        return findByName(name, permissionsAllowedMetaAnnotationOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/deny-all")
    public MyOtherEntity getByNameDenyAll(@RestPath String name) {
        return findByName(name, denyAllMyOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/deny-all-method-with-permit-all")
    public MyOtherEntity getByNameDenyAllMethodWithPermitAll(@RestPath String name) {
        return findByName(name, denyAllMyOtherRepository::findByNamePermitAll);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/parent-method-security")
    public MyOtherEntity getByNameParentMethodSecurity(@RestPath String name) {
        return findByName(name, parentMethodSecurityMyOtherRepository::findByName);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}/parent-class-security")
    public MyOtherEntity getByNameParentClassSecurity(@RestPath String name) {
        return findByName(name, parentClassSecurityMyOtherRepository::findByName);
    }

    private static MyOtherEntity findByName(String name, Function<String, List<MyOtherEntity>> repoFunction) {
        List<MyOtherEntity> entities = repoFunction.apply(name);
        if (entities.isEmpty()) {
            throw new NotFoundException();
        }
        return entities.get(0);
    }

}
