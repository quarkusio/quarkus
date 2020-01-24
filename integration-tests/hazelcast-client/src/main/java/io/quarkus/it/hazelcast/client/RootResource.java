package io.quarkus.it.hazelcast.client;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import com.hazelcast.core.HazelcastInstance;

@Path("/hazelcast-client")
public class RootResource {

    @Inject
    HazelcastInstance hazelcastInstance;

    @POST
    @Path("/ds/put")
    @Produces(MediaType.APPLICATION_JSON)
    public void ds_put(@QueryParam("key") String key, @QueryParam("value") String value) {
        DataSerializableWrapper dataSerializableWrapper = new DataSerializableWrapper();
        dataSerializableWrapper.setValue(value);
        hazelcastInstance.getMap("ds_map").put(key, dataSerializableWrapper);
    }

    @GET
    @Path("/ds/get")
    @Produces(MediaType.APPLICATION_JSON)
    public String ds_get(@QueryParam("key") String key) {
        return hazelcastInstance.<String, DataSerializableWrapper> getMap("ds_map")
                .getOrDefault(key, new DataSerializableWrapper("default")).getValue();
    }

    @POST
    @Path("/ids/put")
    @Produces(MediaType.APPLICATION_JSON)
    public void ids_put(@QueryParam("key") String key, @QueryParam("value") String value) {
        IdentifiedDataSerializableWrapper dataSerializableWrapper = new IdentifiedDataSerializableWrapper();
        dataSerializableWrapper.setValue(value);
        hazelcastInstance.getMap("ids_map").put(key, dataSerializableWrapper);
    }

    @GET
    @Path("/ids/get")
    @Produces(MediaType.APPLICATION_JSON)
    public String ids_get(@QueryParam("key") String key) {
        return hazelcastInstance.<String, IdentifiedDataSerializableWrapper> getMap("ids_map")
                .getOrDefault(key, new IdentifiedDataSerializableWrapper("default")).getValue();
    }

    @POST
    @Path("/ptable/put")
    @Produces(MediaType.APPLICATION_JSON)
    public void ptable_put(@QueryParam("key") String key, @QueryParam("value") String value) {
        PortableWrapper portable = new PortableWrapper("value1");
        portable.setValue(value);
        hazelcastInstance.getMap("ptable_map").put(key, portable);
    }

    @GET
    @Path("/ptable/get")
    @Produces(MediaType.APPLICATION_JSON)
    public String ptable_put_get(@QueryParam("key") String key) {
        return hazelcastInstance.<String, PortableWrapper> getMap("ptable_map")
                .getOrDefault(key, new PortableWrapper("default")).getValue();
    }
}
