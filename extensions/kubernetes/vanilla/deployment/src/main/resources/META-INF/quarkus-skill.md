
### What This Extension Does

Generates Kubernetes manifests (`Deployment`, `Service`, etc.) at **build time** in `target/kubernetes/kubernetes.yml`. No runtime component — manifests are generated during `mvn package` or `gradle build`, not in dev mode.

### Container Image

```properties
quarkus.container-image.registry=myregistry.io
quarkus.container-image.group=myorg
quarkus.container-image.name=myapp
quarkus.container-image.tag=1.0
```

These properties feed into the generated Deployment's container image reference.

### Common Configuration

```properties
# Replicas
quarkus.kubernetes.replicas=2

# Labels (quote keys with dots)
quarkus.kubernetes.labels."app.kubernetes.io/managed-by"=quarkus

# Annotations
quarkus.kubernetes.annotations."prometheus.io/scrape"=true

# Environment variables
quarkus.kubernetes.env.vars.APP_ENV=production
quarkus.kubernetes.env.vars.LOG_LEVEL=INFO

# From ConfigMap
quarkus.kubernetes.env.configmaps=my-config

# From Secret
quarkus.kubernetes.env.secrets=my-secret

# Service type
quarkus.kubernetes.service-type=ClusterIP
```

### Health Probes

```properties
quarkus.kubernetes.liveness-probe.http-action-path=/q/health/live
quarkus.kubernetes.readiness-probe.http-action-path=/q/health/ready
quarkus.kubernetes.startup-probe.http-action-path=/q/health/started
```

Note: the property is `http-action-path`, not just `path`. Add the `smallrye-health` extension for these endpoints to exist.

### Resource Requests and Limits

```properties
quarkus.kubernetes.resources.requests.cpu=100m
quarkus.kubernetes.resources.requests.memory=128Mi
quarkus.kubernetes.resources.limits.cpu=500m
quarkus.kubernetes.resources.limits.memory=256Mi
```

### Volumes and Mounts

```properties
# ConfigMap volume
quarkus.kubernetes.config-map-volumes.config-vol.config-map-name=app-config
quarkus.kubernetes.mounts.config-vol.path=/config

# Secret volume
quarkus.kubernetes.secret-volumes.secret-vol.secret-name=app-secrets
quarkus.kubernetes.mounts.secret-vol.path=/secrets
```

### ServiceAccount and RBAC

```properties
quarkus.kubernetes.service-account=my-service-account

# Role binding
quarkus.kubernetes.rbac.role-bindings.my-binding.role-name=my-role
quarkus.kubernetes.rbac.roles.my-role.policy-rules.0.api-groups=
quarkus.kubernetes.rbac.roles.my-role.policy-rules.0.resources=pods
quarkus.kubernetes.rbac.roles.my-role.policy-rules.0.verbs=get,list,watch
```

### Init Containers

```properties
quarkus.kubernetes.init-containers.init-db.image=busybox:latest
quarkus.kubernetes.init-containers.init-db.command=sh,-c,echo init done
```

### Deploying

```properties
quarkus.kubernetes.deploy=true
```

Set this to deploy directly to the current kubectl context during build. Requires a running cluster and configured `kubeconfig`.

### OpenShift

For OpenShift, use the `quarkus-openshift` extension instead. Configuration uses `quarkus.openshift.*` prefix (same structure as `quarkus.kubernetes.*`).

### Verifying Generated Manifests

After `mvn package`, inspect `target/kubernetes/kubernetes.yml`. The generated manifests are YAML with all configured Deployments, Services, and related resources.

### Common Pitfalls

- Manifests are generated at **build time** (`mvn package`), not in dev mode — don't look for them during `quarkus dev`.
- Label keys with dots must be **quoted**: `quarkus.kubernetes.labels."app.kubernetes.io/name"=myapp`.
- Probe path property is `http-action-path`, not `path` — easy to get wrong.
- `quarkus.container-image.*` properties set the image; `quarkus.kubernetes.*` properties configure the Deployment.
- The `kubernetes` extension generates manifests but does NOT build container images — add a container-image extension (`container-image-docker`, `container-image-jib`, etc.) for that.
