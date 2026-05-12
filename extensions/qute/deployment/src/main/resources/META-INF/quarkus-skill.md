
### Required Extensions

To use Qute with Quarkus REST, you need **three** extensions:
- `quarkus-qute` — the template engine
- `quarkus-rest` — the REST framework
- `quarkus-rest-qute` — the **bridge** between them (WITHOUT this, `TemplateInstance` returns `toString()` instead of rendered HTML — a silent failure with no error message)

### Type-Safe Templates with @CheckedTemplate

```java
@Path("/products")
public class ProductResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance products(List<Product> products);
        public static native TemplateInstance product(Product product);
    }

    @GET @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.products(productService.listAll());
    }

    @GET @Path("/{id}") @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        return Templates.product(productService.findById(id));
    }
}
```

Template files go in `src/main/resources/templates/{EnclosingClass}/{methodName}.html`:
- `templates/ProductResource/products.html`
- `templates/ProductResource/product.html`

### Template Syntax

```html
<!-- Loop -->
{#for product in products}
  <tr><td>{product.name}</td><td>{product.price}</td></tr>
{/for}

<!-- Conditional -->
{#if products.isEmpty}
  <p>No products found.</p>
{#else}
  <table>...</table>
{/if}

<!-- Include layout -->
{#include base}
  {#title}Product List{/title}
  {#content}
    <h1>Products</h1>
    ...
  {/content}
{/include}
```

### Base Layout Template

Create `templates/base.html`:

```html
<!DOCTYPE html>
<html>
<head><title>{#insert title}Default Title{/insert}</title></head>
<body>
  {#insert content}Default content{/insert}
</body>
</html>
```

Child templates use `{#include base}` with `{#title}` and `{#content}` sections.

### Template Extensions

Add custom methods to types usable in templates:

```java
@TemplateExtension
public class StringExtensions {
    static String truncate(String str, int length) {
        return str.length() > length ? str.substring(0, length) + "..." : str;
    }
}
// In template: {product.description.truncate(100)}
```

### Programmatic Templates

Inject and render templates without `@CheckedTemplate`:

```java
@Inject Template products;  // matches templates/products.html

public String render() {
    return products.data("products", productList).render();
}
```

### Template Fragments

Define reusable fragments within templates (useful for HTMX partial updates):

```html
{#fragment id=product-row}
<tr><td>{product.name}</td><td>{product.price}</td></tr>
{/fragment}
```

Render just the fragment:

```java
@CheckedTemplate
public static class Templates {
    public static native TemplateInstance products$product_row(Product product);
}
```

Fragment method name uses `$` separator: `templateName$fragmentId`.

### Testing

- Test HTML responses with REST Assured:
  ```java
  given().get("/products").then()
      .statusCode(200)
      .contentType(containsString("text/html"))
      .body(containsString("Product Name"));
  ```
- Templates are validated at build time — type errors fail compilation.

### Common Pitfalls

- **`quarkus-rest-qute` is required** when using Qute with Quarkus REST. Without it, `TemplateInstance` returns its `toString()` instead of rendered HTML — no error, just wrong output.
- `@CheckedTemplate` must be a **nested static class** inside the resource.
- Template path follows `templates/{EnclosingClass}/{methodName}.html` — wrong path = build error.
- `native` methods in `@CheckedTemplate` are generated at build time — they won't compile outside Quarkus.
- Template expressions are type-checked at build time — typos in `{product.nmae}` cause build failures (not runtime errors).
