package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/employee")
@Produces("application/json")
public class EmployeeResource {

    private final EmployeeRepository employeeRepository;

    public EmployeeResource(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GET
    public List<Employee> findAll() {
        return this.employeeRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public Employee findById(@PathParam("id") Long id) {
        return this.employeeRepository.findById(id).orElse(null);
    }

    @GET
    @Path("/unit/{orgUnitName}")
    public List<Employee> findByManagerOfManager(@PathParam("orgUnitName") String orgUnitName) {
        return this.employeeRepository.findByBelongsToTeamOrganizationalUnitName(orgUnitName);
    }

    @GET
    @Path("/search")
    public List<Employee> findByLastNameContainingAndFirstNameContainingAndEmailContainingAllIgnoreCase(
            @QueryParam("first") String firstName, @QueryParam("last") String lastName, @QueryParam("email") String email) {
        return this.employeeRepository.findByLastNameContainingAndFirstNameContainingAndEmailContainingAllIgnoreCase(lastName,
                firstName, email);
    }

    @GET
    @Path("/search-first-2")
    public List<Employee> findTop2ByFirstNameContainingAllIgnoreCase(@QueryParam("first") String firstName) {
        return this.employeeRepository.findFirst2ByFirstNameContainingIgnoreCaseOrderById(firstName);
    }
}
