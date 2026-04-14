### Bean Validation Annotations

- Use constraints from `jakarta.validation.constraints`/`org.hibernate.validator.constraints` packages, e.g. `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern` on class fields and getters or on method parameters and return values of CDI beans.
- Use `@Valid` for cascading validation: on bean properties or method's return value/parameters represented by a complex type with constraints.
- Place constraints on REST endpoint parameters for automatic 400 responses.

### REST Integration

- Constraint violations on REST endpoint **parameters** automatically return HTTP 400 with validation details.
- Annotate request body DTOs with constraints, and use `@Valid` on the endpoint parameter.
- Return value validation and service method validation result in HTTP 500, not 400 — handle `ConstraintViolationException` explicitly if you need custom error responses for those.

### Custom Validators

- For simple constraints that are only applicable to the current type consider creating a simple boolean getter constrained with `@AssertTrue`: `@AssertTrue(message="{message.key.for.this.check}") public boolean is[NameOfTheCheck]() { ... }`
- For reusable constraints:
    * Create a constraint annotation with `@Target({ METHOD, FIELD, ... }) @Retention(RUNTIME) @Constraint(validatedBy = {})`.
    * Implement `ConstraintValidator<MyAnnotation, TargetType>`.
    * Add a FQCN of the implemented constraint validator to the service file `src/main/resources/META-INF/services/jakarta.validation.ConstraintValidator`

### Method Validation

- Annotate CDI bean method parameters and return values with constraints.
- Method validation is automatic for CDI beans — no extra config needed.

### Testing

- Test validation by sending invalid input via REST Assured and asserting 400 status.
- For unit testing validators, use `Validator` injection and `validator.validate(object)`.

### Common Pitfalls

- Do NOT forget `@Valid` on nested object parameters — Without it, constraints on nested object fields are silently ignored.
- Automatic method parameter/return value validation only works on CDI-managed beans — any method calls on plain `new` objects are not validated.
