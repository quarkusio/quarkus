### Spring DI Compatibility

- `@Component`, `@Service`, `@Repository` — mapped to `@ApplicationScoped`.
- `@Autowired` — mapped to `@Inject`.
- `@Value("${config.key}")` — mapped to `@ConfigProperty`.
- `@Configuration` + `@Bean` — mapped to `@Produces`.

### Quarkus Specifics

- Bean discovery is build-time — same as ArC.
- Constructor injection works without `@Autowired`.

### Common Pitfalls

- Not all Spring DI features are supported (no `@Conditional`, limited `@Profile`).
- Consider using CDI annotations directly for new Quarkus projects.
