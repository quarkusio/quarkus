### Usage

- Add this extension to read configuration from a Spring Cloud Config Server.
- Configure with `quarkus.spring-cloud-config.url=http://config-server:8888`.
- Properties from the config server are merged into the Quarkus configuration at startup.

### Configuration

- Set the application name: `quarkus.spring-cloud-config.name=my-app`.
- Set the profile: `quarkus.spring-cloud-config.profiles=production`.
- Enable/disable: `quarkus.spring-cloud-config.enabled=true`.

### Testing

- Disable in test mode: `%test.quarkus.spring-cloud-config.enabled=false`.
- Use `application.properties` for test configuration.

### Common Pitfalls

- The config server must be running before the application starts — there is no Dev Services for Spring Cloud Config.
- Do NOT enable this in dev mode unless a config server is running locally.
- For Kubernetes-native config, use `quarkus-kubernetes-config` instead.
