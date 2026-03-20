### Typesafe Client

- Define an interface annotated with `@GraphQLClientApi`.
- Methods map to GraphQL queries/mutations based on return type and name.
- Configure: `quarkus.smallrye-graphql-client.my-client.url=http://api/graphql`.

### Dynamic Client

- Inject `DynamicGraphQLClient` for runtime-defined queries.
- Use `client.executeSync(document)` with raw GraphQL strings.

### Testing

- Use `@InjectMock` on the GraphQL client interface.
- Or point the client URL to a test server.

### Common Pitfalls

- Typesafe client generates queries from method signatures — naming conventions matter.
