package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * RESTEasy should be able to use type parameter values (Student, Integer) for (de)marshalling parameters/entity body.
 *
 * @author Jozef Hartinger
 */
@Path("/student")
@Produces("application/student")
@Consumes("application/student")
public class GenericResourceStudentCrudResource extends GenericResourceCrudResource<GenericResourceStudent, Integer> {

    private static Map<Integer, GenericResourceStudent> students = new HashMap<Integer, GenericResourceStudent>();

    public GenericResourceStudentCrudResource() {
        students.put(1, new GenericResourceStudent("Jozef Hartinger"));
    }

    @Override
    GenericResourceStudent getEntity(Integer id) {
        return students.get(id);
    }

    @Override
    void setEntity(Integer id, GenericResourceStudent entity) {
        students.put(id, entity);
    }
}
