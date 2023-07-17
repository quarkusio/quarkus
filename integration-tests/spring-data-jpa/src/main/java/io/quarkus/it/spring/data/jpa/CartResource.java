package io.quarkus.it.spring.data.jpa;

import static io.quarkus.it.spring.data.jpa.CartStatus.CANCELED;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/cart")
@Produces("application/json")
public class CartResource {

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;

    public CartResource(CartRepository cartRepository, CustomerRepository customerRepository) {
        this.cartRepository = cartRepository;
        this.customerRepository = customerRepository;
    }

    @GET
    public List<Cart> findAll() {
        return this.cartRepository.findAll();
    }

    @GET
    @Path("/active")
    public List<Cart> findAllActiveCarts() {
        return this.cartRepository.findByStatus(CartStatus.NEW);
    }

    @GET
    @Path("/customer/{id}")
    public Cart getActiveCartForCustomer(@PathParam("id") Long customerId) {
        return this.cartRepository.findByStatusAndCustomerId(CartStatus.NEW, customerId);
    }

    @GET
    @Path("/{id}")
    public Cart findById(@PathParam("id") Long id) {
        return this.cartRepository.findById(id).orElse(null);
    }

    @POST
    @Path("/customer/{id}")
    public Cart create(@PathParam("id") Long customerId) {
        if (this.getActiveCartForCustomer(customerId) == null) {
            Customer customer = this.customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalStateException("The Customer does not exist!"));

            Cart cart = new Cart(customer, CartStatus.NEW);

            return this.cartRepository.save(cart);
        } else {
            throw new IllegalStateException("There is already an active cart");
        }
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        Cart cart = this.cartRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Cannot find Cart with id " + id));

        cart.setStatus(CANCELED);
        this.cartRepository.save(cart);
    }
}
