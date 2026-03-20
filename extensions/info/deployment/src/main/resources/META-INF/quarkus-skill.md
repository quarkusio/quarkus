### Info Endpoint

- Exposes application info at `/q/info`.
- Includes build info (group, artifact, version) and git info (branch, commit) automatically.

### Custom Info

- Implement `InfoContributor` as a CDI bean to add custom info.

### Configuration

- `quarkus.info.enabled=true` (default).
- `quarkus.info.git.enabled=true` — include git info.
- `quarkus.info.build.enabled=true` — include build info.
