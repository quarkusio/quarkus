
### Annotation Mapping

Spring DI annotations are mapped to CDI at build time:

| Spring | CDI Equivalent | Notes |
|--------|---------------|-------|
| `@Component`, `@Service`, `@Repository` | `@Singleton` | Singleton by default |
| `@Autowired` | `@Inject` | Field, constructor, and setter injection |
| `@Qualifier("name")` | `@Named("name")` | Named bean selection |
| `@Configuration` + `@Bean` | `@Produces` + `@Singleton` | Bean producer methods |
| `@Value("${prop}")` | `@ConfigProperty(name="prop")` | Config injection |
| `@Scope("prototype")` | `@Dependent` | New instance per injection |
| `@Scope("request")` | `@RequestScoped` | Request scope |

### Service Pattern

```java
@Service
public class GreetingService {

    @Value("${greeting.message:Hello}")
    String message;

    public String greet(String name) {
        return message + ", " + name + "!";
    }
}
```

`@Value` supports `${property}` and `${property:default}` syntax. SpEL expressions (`#{...}`) are **not supported**.

### Bean Producers

```java
@Configuration
public class AppConfig {

    @Bean(name = "jsonFormatter")
    public Formatter jsonFormatter() {
        return new JsonFormatter();
    }

    @Bean(name = "xmlFormatter")
    public Formatter xmlFormatter() {
        return new XmlFormatter();
    }
}
```

`@Bean` methods default to singleton scope. Use `@Scope("prototype")` on the method for dependent scope.

### Qualified Injection

```java
@Autowired
@Qualifier("jsonFormatter")
Formatter formatter;
```

The `@Qualifier` value must match the `@Bean(name = "...")` value exactly.

### Constructor Injection

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Mixing Spring and CDI

Spring and CDI annotations interoperate — a `@Service` bean can be injected with `@Inject`, and a CDI `@ApplicationScoped` bean can be injected with `@Autowired`.

### Testing

Spring annotations work in `@QuarkusTest` — use `@Autowired` to inject beans directly:

```java
@QuarkusTest
class GreetingServiceTest {

    @Autowired
    GreetingService service;

    @Test
    void testGreeting() {
        assertEquals("Hello, World!", service.greet("World"));
    }
}
```

### Common Pitfalls

- **SpEL not supported**: `@Value("#{expression}")` throws `IllegalArgumentException`. Use `@Value("${property}")` or `@ConfigProperty` for config values.
- **Default scope is singleton**: Unlike Spring Boot's conditional scanning, `@Component`/`@Service`/`@Repository` always map to `@Singleton` in Quarkus.
- **`@Bean` default is singleton**: Bean producer methods in `@Configuration` classes default to singleton scope, matching Spring behavior.
- **No `@ComponentScan`**: Quarkus discovers beans at build time from the application classpath — no scanning configuration needed.
- **No `@Conditional` support**: Spring's `@Conditional*` annotations are not supported. Use Quarkus build-time conditions or CDI alternatives.
- **This is a compatibility layer**: For new code, consider using CDI annotations (`@ApplicationScoped`, `@Inject`) directly. The Spring DI extension is primarily for migrating existing Spring code.
