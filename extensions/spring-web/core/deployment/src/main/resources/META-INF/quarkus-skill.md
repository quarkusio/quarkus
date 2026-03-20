### Spring Web Compatibility

- `@RestController` — mapped to JAX-RS resource.
- `@GetMapping`, `@PostMapping`, etc. — mapped to `@GET`, `@POST`, etc.
- `@RequestBody` — mapped to JAX-RS entity parameter.
- `@PathVariable` — mapped to `@PathParam`.
- `@RequestParam` — mapped to `@QueryParam`.
- `ResponseEntity<T>` — mapped to `Response`.

### Testing

- Use `@QuarkusTest` with REST Assured — same as JAX-RS testing.

### Common Pitfalls

- Not all Spring Web features are supported (no WebFlux, limited `@ControllerAdvice`).
- Consider using JAX-RS annotations directly for new Quarkus projects.
