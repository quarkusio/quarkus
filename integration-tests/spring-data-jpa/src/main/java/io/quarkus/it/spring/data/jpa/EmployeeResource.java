package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
}
