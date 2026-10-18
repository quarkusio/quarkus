package io.quarkus.it.spring.data.jpa;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/order")
@Produces("application/json")
public class OrderResource {
    private final OrderRepository orderRepository;

    public OrderResource(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GET
    public List<Order> findAll() {
        return this.orderRepository.findAll();
    }

    @GET
    @Path("/customer/{id}")
    public List<Order> findAllByUser(@PathParam("id") Long id) {
        return this.orderRepository.findByCartCustomerId(id);
    }

    @GET
    @Path("/{id}")
    public Order findById(@PathParam("id") Long id) {
        return this.orderRepository.findById(id).orElse(null);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        this.orderRepository.deleteById(id);
    }

    @GET
    @Path("/exists/{id}")
    public boolean existsById(@PathParam("id") Long id) {
        return this.orderRepository.existsById(id);
    }
}
