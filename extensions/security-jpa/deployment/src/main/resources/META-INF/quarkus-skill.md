
### User Entity

Annotate exactly one JPA entity with `@UserDefinition`. It must have fields for username, password, and roles:

```java
@Entity
@Table(name = "app_user") // "user" is a reserved SQL keyword
@UserDefinition
public class User extends PanacheEntity {

    @Username
    public String username;

    @Password // defaults to PasswordType.MCF (BCrypt in Modular Crypt Format)
    public String password;

    @Roles // comma-separated string, e.g. "user,admin"
    public String roles;
}
```

Only **one** entity can have `@UserDefinition` per application. `@Username` and `@Password` fields must be `String`.

### Roles Options

`@Roles` supports three patterns:
- **`String`** — comma-separated roles: `"user,admin"`
- **`Collection<String>`** — list of role strings
- **`Collection<RoleEntity>`** — separate entity with `@RolesValue` on its role name field:

```java
@Entity
public class Role {
    @RolesValue
    public String roleName;
}
```

### Password Hashing

Use `BcryptUtil` from the `elytron-security-common` module (included transitively):

```java
import io.quarkus.elytron.security.common.BcryptUtil;

user.password = BcryptUtil.bcryptHash("plaintextPassword");
// overloads: bcryptHash(password, iterationCount) and bcryptHash(password, iterationCount, salt)
```

`PasswordType.MCF` (default) expects BCrypt in Modular Crypt Format. Other options:
- `@Password(PasswordType.CLEAR)` — cleartext, **never use in production**
- `@Password(value = PasswordType.CUSTOM, provider = MyProvider.class)` — implement `PasswordProvider`

### Seeding Users

```java
@ApplicationScoped
public class UserSeeder {
    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        if (User.count() == 0) {
            User user = new User();
            user.username = "alice";
            user.password = BcryptUtil.bcryptHash("password");
            user.roles = "user";
            user.persist();
        }
    }
}
```

The startup observer must be `@Transactional` to persist entities.

### Configuration

```properties
# Enable HTTP Basic authentication
quarkus.http.auth.basic=true
```

No security-jpa-specific configuration is needed — the extension auto-detects the `@UserDefinition` entity.

### Securing Endpoints

```java
@Path("/api")
public class ApiResource {

    @GET @Path("/public") @PermitAll
    public String publicEndpoint() { return "public"; }

    @GET @Path("/user") @RolesAllowed("user")
    public String userEndpoint() { return "user content"; }

    @GET @Path("/admin") @RolesAllowed("admin")
    public String adminEndpoint() { return "admin content"; }
}
```

### Testing

Use preemptive basic auth in REST Assured — Quarkus returns 401 before the body otherwise:

```java
given()
    .auth().preemptive().basic("alice", "password")
    .when().get("/api/user")
    .then().statusCode(200);
```

Expect 401 (Unauthorized) for missing credentials, 403 (Forbidden) for wrong role.

### Common Pitfalls

- **Table name "user"** — reserved in most SQL dialects. Use `@Table(name = "app_user")`.
- **Non-preemptive auth in tests** — REST Assured default waits for a 401 challenge first, which breaks some test assertions. Always use `.auth().preemptive().basic(...)`.
- **Missing `@Transactional` on seeder** — persisting entities in a startup observer without `@Transactional` may silently fail. In production, set `quarkus.datasource.jdbc.transaction-requirement=STRICT` to enforce transaction boundaries.
- **`@Roles` is not limited to String** — also supports `Collection<String>` and entity relationships with `@RolesValue`.
- **Only one `@UserDefinition` entity allowed** — a build error occurs if multiple entities have this annotation.
