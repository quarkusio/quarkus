### Templates

- Place template files in `src/main/resources/templates/`.
- Inject with `@Inject Template myTemplate` — name matches filename (e.g. `myTemplate.html`).
- Render with `template.data("name", value).render()`.

### Type-Safe Templates

- Use `@CheckedTemplate` on a static inner class for compile-time validated templates.
- Template parameters are checked at build time — typos are caught early.

### Template Syntax

- Expressions: `{name}`, `{item.price}`.
- Sections: `{#if condition}...{/if}`, `{#for item in items}...{/for}`.
- Comments: `{! this is a comment !}`.
- Include: `{#include base}{/include}`.

### REST Integration

- Return `TemplateInstance` from REST endpoints for server-rendered HTML.

### Testing

- Inject templates in `@QuarkusTest` and assert rendered output.

### Common Pitfalls

- Template file names must match the injection point name (or use `@Location`).
- Type-safe templates require the template file to exist at build time.
