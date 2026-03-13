# Author-driven guide navigation from adoc attributes

> [!NOTE]
> This builds on @gsmet's [Reorganizing our documentation](https://github.com/quarkusio/quarkus/discussions/52702).
> That discussion calls for curated categorization with subcategories, build-time
> validation, and compact navigation — all enforced by tooling.
> This proposal addresses the *mechanism*: how categories and subcategories
> flow from adoc source files into the landing page and sidebar.

## The problem

Navigation is manually maintained in `quarkusio.github.io/_data/domains.yaml`
(43 KB, 15 domains, ~280 guide entries). When someone adds or renames a guide
in the main repo, they must separately edit a YAML file in a different repo.
Nobody enforces that the two stay in sync, and they frequently don't.

Meanwhile, every guide already declares a `:categories:` attribute. The build
scrapes it, validates it — and throws it away. It has no effect on navigation.

## The proposal

Make navigation fully author-driven:

1. **`:categories:` drives domain assignment.** Each value maps to a navigation
   domain. A guide with `:categories: web, data` appears in both.

2. **`:subcategories:` drives grouping.** A new optional attribute. A guide with
   `:subcategories: openshift` appears under the "OpenShift" subcategory group
   within its parent domain.

3. **`:nav-title:` provides the sidebar label.** An optional short title (≤ 40
   characters) for compact sidebar display. When omitted, the document title is
   used automatically. Required only when the title exceeds 40 characters.

4. **A configuration file defines the vocabulary.** Valid categories,
   subcategories, display titles, featured guides, and display order live in
   `docs/src/main/asciidoc/navigation-config.yaml` — in the main repo, next to
   the guides it governs.

5. **The build validates and generates.** `YamlMetadataGenerator` reads the
   config, validates every guide's attributes against it, applies a placement
   rule, and outputs `navigation.yaml` — the same structure templates already
   consume.

## Configuration file

`navigation-config.yaml` lives alongside the guides and defines the navigation
vocabulary:

```yaml
title-limit: 40
featured-limit: 4

categories:
  - category: security
    cat-title: Secure Your Application
    use-case: Add authentication, authorization, and endpoint protection.
    subcategories:
      - subcategory: oidc
        subcat-title: OIDC & OAuth

  - category: cloud
    cat-title: Deploy and Run in the Cloud
    subcategories:
      - subcategory: openshift
        subcat-title: Deploy to OpenShift

  # A subcategory can appear under multiple categories.
  - category: native
    cat-title: Compile to Native Executables
    subcategories:
      - subcategory: openshift
        subcat-title: Deploy to OpenShift

featured:
  - file: getting-started.adoc
    featured-summary: Build a hello-world app in 10 minutes
```

Category order in this file determines display order on the landing page and
sidebar. Guides within each category/subcategory are sorted alphabetically by
`nav-title`.

## Placement rule

When a guide specifies both `:categories:` and `:subcategories:`, placement
depends on whether the subcategory belongs to the category:

| Situation | Result |
|---|---|
| Subcategory belongs to the category | Guide appears **only** under that subcategory group (not also flat). Avoids duplication. |
| Subcategory is unrelated to the category | Guide appears **both** flat under its category **and** grouped under the subcategory's parent. |
| Guide has only `:categories:` | Flat under each listed category. |
| Guide has only `:subcategories:` | Grouped under each subcategory's parent category. |

### Example

```asciidoc
= Deploy a native executable to OpenShift
:categories: native
:subcategories: openshift
:nav-title: Native on OpenShift
```

OpenShift is defined under Cloud (not Native) in the config. So this guide
appears:
- **Flat** under "Compile to Native Executables"
- **Grouped** under "Deploy and Run in the Cloud" → "Deploy to OpenShift"

## Validation

The build (strict by default) fails with an instructive error if:
- `:categories:` is missing or contains an unknown value
- `:subcategories:` contains an unknown value
- `:nav-title:` exceeds the character limit, or the title exceeds the limit and
  no `:nav-title:` is provided

A lenient mode (`-Dvalidation=lenient`) converts these to warnings for local
iteration.

## Author workflow

**Adding a new guide:**
1. Add `:categories:` and optionally `:subcategories:` to the document header.
   Add `:nav-title:` only if the document title exceeds 40 characters.
2. Run `./mvnw -DquicklyDocs` — the build catches any invalid values.
3. Open a PR. CI enforces strict validation.

**Adding a new category or subcategory:**
1. Add the entry to `navigation-config.yaml`.
2. Update the relevant guides.
3. Open a PR.

No manual editing of `domains.yaml` in quarkusio.github.io. The generated
`navigation.yaml` replaces it.

## Learning paths

As @gsmet proposed in #52702, users need curated sequences of guides that address
a specific goal. Unlike categories (sorted alphabetically by nav-title),
learning paths list guides in an explicit, author-defined order.

Learning paths are defined in `navigation-config.yaml`:

```yaml
learning-paths:
  - path: rest-crud
    path-title: Build a RESTful CRUD application
    path-summary: Build a complete REST API with database access and security.
    guides:
      - rest.adoc
      - hibernate-orm-panache.adoc
      - security-overview.adoc
      - security-authorize-web-endpoints-reference.adoc
```

- **`path`** — a unique ID for the learning path.
- **`path-title`** — display heading.
- **`path-summary`** — optional one-line description of the goal.
- **`guides`** — ordered list of adoc filenames. The order is the display order.

The build validates that there are no duplicate path IDs or duplicate guides
within a path. At generation time, the build warns about any listed file not
found among processed guides. The generated `navigation.yaml` includes a
`learning-paths` section with full guide metadata (url, title, diataxis-type,
nav-title) in the specified order.

## Migration

The initial `navigation-config.yaml` is seeded from the existing Category enum,
so every current `:categories:` value is valid from day one. Migration is
incremental — one guide or one category at a time — without breaking the build.

A follow-up PR will illustrate the mechanism in practice by restructuring a
crowded category (e.g., security) with subcategories, updated nav-titles, and
refined category assignments.

> [!NOTE]
> A handful of guides on `main` have `:categories:` values that don't match any
> entry in the Category enum (e.g., `analytics`, `extensions`, `initialization`,
> `rule engine`, `reactive`). These need to be audited and fixed before strict
> validation is enabled. A cleanup PR should land first or alongside the initial
> config PR.

## Open questions for discussion

1. **Category inventory.** The current enum has 20 categories. What categories
   should the landing page have? Which categories should be consolidated or
   split? This is a separate discussion from the mechanism itself.

2. **Template key renames.** The output YAML renames `domains` → `navigation`,
   `id` → `category`, `title` → `cat-title`, `job` → `use-case`, `type` →
   `diataxis-type`. Should the rename happen in the same PR or a follow-up?

---

cc @gsmet — this is the mechanism behind the categorization tooling you
described in #52702.
