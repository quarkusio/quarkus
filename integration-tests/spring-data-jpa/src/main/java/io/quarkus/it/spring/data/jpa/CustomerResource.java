package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/customer")
@Produces("application/json")
public class CustomerResource {

    private final CustomerRepository customerRepository;

    public CustomerResource(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GET
    public List<Customer> findAll() {
        return this.customerRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public Customer findById(@PathParam("id") Long id) {
        return this.customerRepository.findById(id).orElse(null);
    }

    @GET
    @Path("/active")
    public List<Customer> findAllActive() {
        return this.customerRepository.findAllByEnabled(true);
    }

    @GET
    @Path("/inactive")
    public List<Customer> findAllInactive() {
        return this.customerRepository.findAllByEnabled(false);
    }

    @POST
    public Customer create(Customer customer) {
        return this.customerRepository.save(customer);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        Customer customer = this.customerRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find Customer with id " + id));

        customer.setEnabled(false);
        this.customerRepository.save(customer);
    }
}
