package io.quarkus.deployment;

/**
 * Classifies runtime-init services into ordered phases of application startup.
 * <p>
 * Each phase represents a logical stage in the progression from "configuration loaded" to
 * "application actively serving requests." Phases impose a <em>partial ordering constraint</em>
 * on the service dependency graph: a service may only depend on services in the same phase or
 * an earlier phase. The runtime enforces this constraint at build time; a dependency that
 * violates it is a build error.
 * <p>
 * <strong>Phases are not barriers.</strong> The runtime does not wait for all services in
 * phase <em>N</em> to complete before starting services in phase <em>N+1</em>. Services
 * start as soon as their individual dependencies are satisfied, so services from different
 * phases may execute concurrently. The phase classification constrains which dependency
 * relationships are legal, but within those constraints, startup proceeds with maximum
 * parallelism. Think of phases as a monotonic progression through the startup sequence,
 * not as synchronized checkpoints.
 * <p>
 * Phases also serve as control points for startup modes that do not require a full
 * application lifecycle:
 * <ul>
 * <li><strong>Init-and-exit</strong> ({@code quarkus.init-and-exit=true}): runs through
 * {@link #INIT}, then stops. Migrations and validation tasks complete; the application
 * never serves requests. {@link #BINDING} is skipped (controlled by runtime flag).</li>
 * <li><strong>AppCDS generation</strong>: runs through {@link #INFRASTRUCTURE} (or less),
 * then exits after dumping the class archive.</li>
 * <li><strong>CRaC checkpoint</strong>: runs through a chosen phase, takes a checkpoint,
 * then resumes from the next phase on restore.</li>
 * <li><strong>Dev mode reload</strong>: lower phases persist across hot reloads; upper
 * phases are torn down and rebuilt when application code changes.</li>
 * </ul>
 *
 * @see io.quarkus.core.deployment.action.ActionBuilder
 */
public enum Phase {

    /**
     * Static initialization.
     * <p>
     * Services in this phase run during the static initialization of the generated
     * application class ({@code <clinit>}). Their results are available to all
     * subsequent phases. In native image mode, static-init values are baked into
     * the image heap.
     * <p>
     * Stop handlers registered via {@link io.quarkus.core.StartContext#onStop(Runnable)}
     * run during application shutdown, providing symmetric teardown.
     */
    STATIC_INIT,

    /**
     * Configuration sources and mappings.
     * <p>
     * Services in this phase make runtime configuration available to all subsequent phases.
     * This includes registering configuration property classes, validating runtime config
     * values, and reporting deprecated or unknown configuration.
     * <p>
     * Configuration <em>source</em> setup (parsing property files, environment variables,
     * etc.) occurs during static init. This phase handles runtime-specific configuration
     * tasks that depend on the values being fully resolved.
     */
    CONFIG,

    /**
     * Log manager reconfiguration with runtime configuration.
     * <p>
     * The log manager itself is installed during static init with bootstrap defaults.
     * Services in this phase reconfigure it with the real runtime configuration: setting
     * log levels per category, installing log handlers, applying formatters, and
     * integrating the startup banner.
     * <p>
     * Extensions that export logs through higher-layer mechanisms should install a
     * <em>queued handler</em> during this phase and attach the actual drain target in
     * a later phase when its dependencies are available. This avoids circular
     * dependencies between logging and infrastructure while ensuring no log records
     * are lost during the gap.
     */
    LOGGING,

    /**
     * Core runtime plumbing.
     * <p>
     * Services in this phase create the foundational infrastructure that higher phases
     * depend on: event loops, thread pool executors, TLS registries, network transport
     * configuration, transaction managers, observability SDKs, context propagation
     * mechanisms, and security providers.
     * <p>
     * Infrastructure services are the most stable across dev mode reloads and are
     * expected to survive classloader reconstruction in most cases.
     */
    INFRASTRUCTURE,

    /**
     * Server socket and transport binding.
     * <p>
     * Services in this phase bind server sockets, reserve ports, and establish transport
     * endpoints. Binding happens early so that port conflicts are detected before
     * expensive initialization work in later phases.
     * <p>
     * Binding does <em>not</em> start accepting connections; that happens in
     * {@link #SERVING}. The bound sockets are inert until the application is fully
     * wired and ready to handle requests.
     * <p>
     * In dev mode, bound sockets persist across reloads — the port stays open while
     * handlers are recycled. In init-and-exit mode, this phase is skipped entirely
     * (controlled by a runtime flag) since there is no point binding a socket that
     * will never serve.
     */
    BINDING,

    /**
     * Data connections and resource pools.
     * <p>
     * Services in this phase establish connections to external data systems: connection
     * pools, client connections, directory services, and similar resources. These
     * connections are the foundation for both initialization tasks (migrations, schema
     * validation) and ongoing application data access.
     * <p>
     * Security realms that depend on external data sources also belong in this phase,
     * since their construction requires an established data connection.
     */
    DATA,

    /**
     * One-time initialization tasks.
     * <p>
     * Services in this phase perform work whose value is in its durable side effect:
     * database schema migrations, schema validation, persistence layer startup, cache
     * warming, security domain assembly, and eager configuration validation. Once an
     * init task completes, its outcome survives process exit.
     * <p>
     * In init-and-exit mode, startup proceeds through this phase and then stops —
     * all initialization tasks complete, but the application never enters
     * {@link #APPLICATION} or {@link #SERVING}.
     */
    INIT,

    /**
     * Application-level wiring.
     * <p>
     * Services in this phase register the application's request-handling and event-processing
     * logic: routes and filters, authentication mechanisms, service handlers, event
     * consumers, and scheduled task definitions.
     * <p>
     * This is typically the largest phase. Most extensions that provide user-facing
     * functionality register their handlers here.
     */
    APPLICATION,

    /**
     * Start accepting work.
     * <p>
     * Services in this phase transition the application from "initialized" to "actively
     * processing." The specific mechanism depends on the deployment model: accepting
     * connections on bound sockets, polling message brokers, starting serverless
     * invocation loops, or firing scheduled tasks.
     * <p>
     * This phase is skipped in init-and-exit mode, during class data archive generation,
     * and at checkpoint boundaries. It is the last phase in a normal startup sequence.
     */
    SERVING;
}
