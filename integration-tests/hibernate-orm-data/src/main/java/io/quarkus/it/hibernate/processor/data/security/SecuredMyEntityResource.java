package io.quarkus.it.hibernate.processor.data.security;

import java.util.Arrays;
import java.util.List;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.it.hibernate.processor.data.pudefault.MyEntity;
import io.quarkus.it.hibernate.processor.data.pudefault.MyEntity_;
import io.quarkus.it.hibernate.processor.data.pudefault.SecuredMyRepository;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/secured/data/")
public class SecuredMyEntityResource {

    @Inject
    SecuredMyRepository repository;

    @Inject
    HttpHeaders httpHeaders;

    @Path("/insert-root")
    @POST
    @Transactional
    public void insertForRootRole(MyEntity entity) {
        repository.insertRootRole(entity);
    }

    @Path("/insert-admin")
    @POST
    @Transactional
    public void insertForAdminRole(MyEntity entity) {
        repository.insertAdminRole(entity);
    }

    @Path("/insert-deny-all")
    @POST
    @Transactional
    public void insertDenyAll(MyEntity entity) {
        repository.insertDenyAll(entity);
    }

    @Path("/insert-authenticated")
    @POST
    @Transactional
    public void insertAuthenticated(MyEntity entity) {
        repository.insertAuthenticated(entity);
    }

    @Path("/insert-public")
    @POST
    @Transactional
    public void insertForEveryone(MyEntity entity) {
        repository.insert(entity);
    }

    @Path("/list-all-donald-roles-allowed")
    @GET
    public List<MyEntity> listAllForDonaldRolesAllowed() {
        return repository.findAllForDonald(Order.by(Sort.asc(MyEntity_.NAME))).toList();
    }

    @Path("/list-all-donald-permissions-allowed")
    @GET
    public List<MyEntity> listAllForDonaldPermissionsAllowed(@RestQuery String name) {
        return repository.findAllForDonald(name, Sort.asc(MyEntity_.NAME)).toList();
    }

    @Path("/list-all-donald-public")
    @GET
    public List<MyEntity> listAllForDonaldPublic(@RestQuery String name) {
        return repository.findAllForDonald(Sort.asc(MyEntity_.NAME), name).toList();
    }

    @Path("/list-all-george")
    @GET
    public List<MyEntity> listAllForGeorge() {
        return repository.findAllForGeorge(Order.by(Sort.asc(MyEntity_.NAME))).toList();
    }

    @GET
    @Transactional
    @Path("/by/name/{name}")
    public MyEntity getByName(@RestPath String name) {
        List<MyEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            throw new NotFoundException();
        }
        return entities.get(0);
    }

    @POST
    @Transactional
    @Path("/rename-1/{before}/to/{after}")
    public void rename1(@RestPath String before, @RestPath String after) {
        MyEntity byName = getByName(before);
        byName.name = after;
        repository.rename1(byName);
    }

    @POST
    @Transactional
    @Path("/rename-2/{before}/to/{after}")
    public void rename2(@RestPath String before, @RestPath String after) {
        MyEntity byName = getByName(before);
        byName.name = after;
        repository.rename2(byName);
    }

    @POST
    @Transactional
    @Path("/rename-all-perms-1-2")
    public void renameAllPermissions1And2(@RestQuery String[] before, @RestQuery String[] after) {
        repository.renameAll1And2(loadAndRenameEntities(before, after));
    }

    @POST
    @Transactional
    @Path("/rename-all-perms-2-3")
    public void renameAllPermissions2And3(@RestQuery String[] before, @RestQuery String[] after) {
        repository.renameAll2And3(loadAndRenameEntities(before, after));
    }

    @POST
    @Transactional
    @Path("/rename-overloaded-secured")
    public void renameOverloadedSecured(@RestQuery String[] before, @RestQuery String[] after) {
        repository.renameOverloaded(loadAndRenameEntities(before, after));
    }

    @POST
    @Transactional
    @Path("/rename-overloaded-public")
    public void renameOverloadedPublic(@RestQuery String before, @RestQuery String after) {
        MyEntity byName = getByName(before);
        byName.name = after;
        repository.renameOverloaded(byName);
    }

    @DELETE
    @Transactional
    @Path("/by/name/{name}")
    public void deleteByName(@RestPath String name) {
        repository.delete(name);
    }

    @Path("/insert-all-1")
    @POST
    @Transactional
    public void insertAll1(MyEntity entity) {
        repository.insertAll1(List.of(entity));
    }

    @Path("/insert-all-2")
    @POST
    @Transactional
    public void insertAll2(MyEntity entity) {
        repository.insertAll2(List.of(entity));
    }

    private List<MyEntity> loadAndRenameEntities(@RestQuery String[] before, @RestQuery String[] after) {
        if (before.length != after.length) {
            throw new BadRequestException("Before and after name lengths do not match");
        }
        MyEntity[] entities = new MyEntity[before.length];
        for (int i = 0; i < before.length; i++) {
            entities[i] = getByName(before[i]);
            entities[i].name = after[i];
        }
        return Arrays.asList(entities);
    }

    @PermissionChecker("find-for-donald")
    boolean canFindForDonald(SecurityIdentity ignored) {
        // grant permission "find-for-donald" if user requested it via header
        return "find-for-donald".equals(httpHeaders.getHeaderString("dynamic-permission"));
    }
}
