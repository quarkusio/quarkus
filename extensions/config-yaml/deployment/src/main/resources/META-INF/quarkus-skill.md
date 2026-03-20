### YAML Configuration

- Use `application.yaml` (or `application.yml`) instead of `application.properties`.
- Both formats can coexist — properties take precedence.

### Profile-Specific Config

- Use `"%dev":` or `"%test":` keys for profile-specific values.

### Common Pitfalls

- YAML indentation matters — use spaces, not tabs.
- Quarkus config keys use dots — map them to nested YAML keys.
