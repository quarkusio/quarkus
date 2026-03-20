### GraphQL API

- Annotate a CDI bean with `@GraphQLApi`.
- Use `@Query` for read operations, `@Mutation` for write operations.
- Use `@Subscription` with `Multi<T>` return type for real-time data.
- Use `@Source` on a parameter to define field resolvers.

### GraphQL UI

- Available at `/q/graphql-ui` in dev mode for interactive testing.

### Configuration

- `quarkus.smallrye-graphql.schema-include-directives=true` — include directives in schema.

### Testing

- Use REST Assured to POST GraphQL queries to `/graphql`.
- Send JSON body: `{"query": "{ myQuery { field1 field2 } }"}`.

### Common Pitfalls

- GraphQL types must be POJOs or records — complex types need proper serialization.
- `@Query` and `@Mutation` methods must be on `@GraphQLApi` beans.
