### Usage

- Add this extension for automatic Kubernetes Service Binding support.
- Service bindings are read from the file system (mounted by the Service Binding Operator) and mapped to Quarkus config properties.
- Supports automatic configuration of datasources, messaging, and other services.

### How It Works

- The Service Binding Operator mounts binding data as files in a well-known directory.
- This extension reads those files and maps them to Quarkus configuration properties automatically.
- Supported binding types: postgresql, mysql, mongodb, kafka, redis, and more.

### Testing

- Service bindings are only available in Kubernetes — use standard Quarkus config for local dev/test.
- Dev Services provides backing services automatically in dev/test mode.

### Common Pitfalls

- This extension only works when running in a Kubernetes cluster with the Service Binding Operator installed.
- Do NOT rely on this for local development — use Dev Services or `application.properties` instead.
