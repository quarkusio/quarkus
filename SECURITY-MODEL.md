# Quarkus security model

> [!IMPORTANT]
> This is a draft for discussion.
> It does not yet describe an accepted Quarkus policy.
> See [quarkusio/quarkus#55732](https://github.com/quarkusio/quarkus/discussions/55732).

This draft proposes security boundaries for maintainer discussion and describes information that can help assess a security report.
It deliberately stays at the level of component families, actors, trust boundaries, and reachability.
A catalog of every extension, endpoint, method, or configuration property would be out of date almost immediately.

This model complements, but does not replace, the canonical [Quarkus security policy](https://quarkus.io/security/), which is mirrored in [SECURITY.md](SECURITY.md).
That policy remains the authority for private reporting, supported versions, and disclosure handling.
If you are unsure whether an issue fits this model, report it privately anyway.

## Status and evidence

**Status:** draft

Statements marked *(documented)* are backed by existing Quarkus documentation or policy.
Statements marked *(inferred)* are interpretations that point to an open question.
Except for the documented statements, this entire model, including its scope, trust assumptions, responsibility split, triage outcomes, and non-goals, is a proposal for maintainer review.

## What this model covers

Quarkus moves a substantial amount of work from application startup to build time and supports both JVM and native applications.
Its security surface is therefore more than the HTTP-facing runtime.
It includes the tools that select extensions, the augmentation process that generates an application, the packaged runtime, and the developer surfaces used to build and test it. *(documented: `README.md`, `docs/src/main/asciidoc/writing-extensions.adoc`)*

The model groups that surface by trust profile:

| Component family | Typical inputs and boundaries | Intended scope |
| --- | --- | --- |
| Developer tooling and extension discovery | CLI commands, Maven and Gradle projects, codestarts, registries, catalogs, artifact repositories | Quarkus-owned tooling and its handling of supported inputs |
| Bootstrap, augmentation, and application generation | Application metadata, build-time configuration, deployment artifacts, build steps, bytecode generation and transformation | Quarkus-owned build behavior and generated output |
| Production runtime core | Startup, Arc/CDI, runtime configuration, class loading, common runtime infrastructure | Packaged Quarkus applications in supported production modes |
| Inbound protocols and data formats | HTTP, REST, WebSockets, gRPC, GraphQL, events, messages, parsing, deserialization and validation | Quarkus-owned processing before or around application code |
| Security and identity | Credentials, tokens, certificates, cookies, authentication mechanisms, identity providers, authorization | Quarkus security mechanisms and their documented configuration |
| Data and external-system integrations | Databases, brokers, identity providers, caches, mail, search, cloud and platform services, filesystems | Quarkus integration behavior; upstream defects can require upstream handling |
| Development and test tooling | Dev mode, live reload, Dev UI, JSON-RPC, remote dev, Dev Services, test framework | Developer assets and environments, under a posture distinct from production |
| Independent projects in this repository | Arc, Qute, RESTEasy Reactive and other independently consumable projects | Use through Quarkus is covered; standalone scope remains to be decided |
| Community extensions and upstream components | Quarkiverse/community extensions, including external Quarkus Platform members, and integrated third-party libraries | The generic extension baseline and Quarkus integration paths; code ownership and support must be established separately |

The Quarkus project's own CI, release credentials, artifact signing, and publication infrastructure are not described by this product-focused draft.
Maintainers should decide whether they belong here or in a separate operational model. *(inferred)* [Q9]

This grouping is intentional.
A security issue can be specific to one extension without requiring the model to enumerate that extension.
JVM fast-jar, legacy-jar, AOT, and native execution are cross-cutting variants rather than separate component families.
A report should name the affected variant when it changes reachability or impact. *(inferred)* [Q4]

Catalog presence, maturity level, and Quarkiverse hosting do not by themselves establish Quarkus-team ownership or support.
Quarkus Platform membership is a stronger relationship: the extension is part of a curated, version-aligned set with a compatibility promise.
It still does not by itself establish ownership of the extension code or a particular support scope. *(documented: `SECURITY.md`, `docs/src/main/asciidoc/platform.adoc`, `docs/src/main/asciidoc/extension-metadata.adoc`)*

Whether independent projects in this repository are covered when consumed outside Quarkus remains open. *(inferred)* [Q7]

## Extensions and platform releases

A Quarkus extension can contribute code in two phases:

- its deployment artifact participates in augmentation and can inspect build inputs, run build steps, generate or transform bytecode, record state, and influence the packaged application; and
- its runtime artifact executes as part of the application.

*(documented: `docs/src/main/asciidoc/writing-extensions.adoc`)*

The deployment/runtime split is a lifecycle and class-loading design.
Both parts are executable dependencies; neither is a sandbox for mutually hostile code. *(inferred)* [Q2]

An application developer or build operator that selects an extension grants its deployment code the privileges available to the build process and its runtime code the privileges available to the application process.
Quarkus cannot generally prevent deliberately malicious selected code from using those privileges.
This does not make extension metadata, archives, descriptors, configuration, generated resources, protocol input, or external data inherently trusted.
Quarkus-owned parsing and integration boundaries, and the extension's handling of less-privileged input, remain subject to security review. *(inferred)* [Q1]

For an extension without its own security model, this document proposes a generic fallback profile.
It is an analysis and triage baseline, not a claim that the extension has been audited or complies with it.
An extension-specific model can refine the actors, inputs, modes, properties, and responsibilities, but should make deviations explicit.
The fallback considers at least:

- behavior during augmentation and at runtime;
- production, development, test, JVM, and native reachability;
- attacker-controlled requests, messages, files, metadata, and responses from external systems;
- identity, authorization, tenant, credential, and secret handling;
- outbound connections, generated output, and host-side effects; and
- resource consumption and the split between Quarkus integration, extension-maintainer, application, and operator responsibility.

Using the fallback does not make an externally maintained extension a Quarkus-owned component.
A vulnerability reachable through that extension can still be real and handled privately even when final remediation or disclosure is coordinated with its maintainer. *(inferred)* [Q5]

There are two related BOM layers in a typical Quarkus release:

- `io.quarkus:quarkus-bom` is produced from this repository and manages the extensions released from it, their deployment/runtime artifacts, and their dependency constraints; and
- the official Quarkus Platform release is represented by one or more BOMs under `io.quarkus.platform`, including `io.quarkus.platform:quarkus-bom`.
  A platform BOM is based on a chosen `io.quarkus:quarkus-bom` and can additionally manage external extension artifacts and the third-party versions needed to keep the platform members compatible.

*(documented: `bom/application/pom.xml`, `docs/src/main/asciidoc/platform.adoc`, `docs/src/main/asciidoc/maven-tooling.adoc`)*

The version constraints, platform descriptor, and any platform properties are part of the platform release surface.
Platform membership therefore provides more evidence than catalog presence alone: the platform publisher has selected and aligned those versions and presents their combination as compatible.
It does not turn third-party code into Quarkus-owned code, establish that the code was security-audited, or remove the application's need to trust executable dependencies. *(inferred)* [Q5]

For responsibility and triage, a defect in external extension code remains with that extension's maintainer.
A defect in platform version selection, alignment, descriptor metadata, platform defaults, or the Quarkus-specific integration can also require a Quarkus Platform or Quarkus-side response.
In practice a single report can therefore require both an upstream code fix and a platform BOM update. *(inferred)* [Q5]

## Modes are part of the boundary

Production, development, test, and remote-development modes are not interchangeable:

- Dev mode has a different architecture, retains build-time facilities, and must never be used in production.
  Dev UI can expose confidential state and state-changing operations. *(documented: `docs/src/main/asciidoc/dev-mode-differences.adoc`, `docs/src/main/asciidoc/dev-ui.adoc`)*
- Dev Services provision services in development and test, not in a packaged production application. *(documented: `docs/src/main/asciidoc/dev-services.adoc`)*
- Remote dev is a development facility with its own authentication and transport boundary; it is explicitly not a production deployment mode. *(documented: `docs/src/main/asciidoc/maven-tooling.adoc`)*
- Native executables are built from a production application, but native build and runtime details can still make a finding native-specific. *(documented: `docs/src/main/asciidoc/dev-mode-differences.adoc`, `docs/src/main/asciidoc/building-native-image.adoc`)*

A path that exists only in a test or dev fixture does not establish production reachability.
Conversely, “dev only” does not make every issue harmless: workspace modification, disclosure of developer credentials, compromise of a development host, or poisoning generated production output can be meaningful impact within the developer-tooling boundary.
The exact guarantees for that boundary remain a maintainer decision. *(inferred)* [Q3]

## Assets

The assets considered by this model are:

- the integrity of dependency and extension selection, application augmentation, generated bytecode, recorded state, and packaged artifacts;
- application source, resources, generated files, and the developer workspace;
- runtime and build configuration, secrets, credentials, tokens, keys, and certificates;
- authentication identities, roles, authorization decisions, and session or token state;
- application and business data in requests, messages, files, databases, caches, and external services;
- the confidentiality and integrity of outbound connections and data passed to integrated systems;
- availability of the build and runtime process, host, container, and bounded resources; and
- host or external resources reachable with the privileges of the build or application process.

## Actors and working trust assumptions

The relevant actors are application developers, build and CI operators, runtime operators, extension and dependency publishers, unauthenticated remote clients, authenticated but potentially malicious users or peers, external services and data sources, local developers, and parties able to reach a development endpoint.

Trust is specific to a capability and an asset, not a blanket label for an actor or every byte that actor supplies.
The first version proposes the following distinctions. *(inferred)* [Q1]

| Actor or input | Proposed authority or assumption | What that does not imply |
| --- | --- | --- |
| Application developer and build operator | May select source, dependencies, extensions, plugins, and build configuration | Quarkus may still need to safely parse metadata, archives, paths, descriptors, and generated resources, and must not use that authority against unrelated assets |
| Selected application, extension, dependency, or plugin code | Executes with the privileges granted to the build or runtime process; no sandbox is assumed | Malformed metadata or content is not automatically safe, and Quarkus-owned parsing, selection confusion, path traversal, confused-deputy behavior, or privilege crossing can still be in scope |
| Runtime operator | Chooses deployment mode, network exposure, configuration, external endpoints, credentials, and limits | Operator control does not excuse a misleading default, broken Quarkus enforcement, or mishandling of less-privileged input |
| Remote client or authenticated user | Has only the capabilities granted by the configured application identity and policy | Authentication does not make request, message, tenant, or object input benign |
| External service or data source | The operator may trust and select the endpoint | Its responses, messages, records, filenames, and identity assertions can contain attacker-influenced data |
| Local or remote developer client | May invoke the development facilities deliberately exposed to it | Other websites, network peers, local processes, tenants, or projects do not automatically share those capabilities |

A compromised host or deliberate behavior by code already executing with its granted build or runtime privileges is normally a supply-chain, host, or application concern.
A path by which a less-privileged actor gains that execution or makes Quarkus misuse the privilege remains in scope.

Quarkus class-loader separation is an architectural and lifecycle boundary.
It must not be treated as a security sandbox without an explicit security guarantee. *(inferred)* [Q2]

## Trust boundaries and flows

The proposed main boundaries are:

1. **Registry and repository to build tooling:** catalog and artifact metadata influence dependency selection and the deployment/runtime artifacts used by the build.
2. **Platform release to application dependency graph:** imported BOM constraints, descriptors, and platform properties select and align deployment artifacts, runtime artifacts, transitive dependencies, and some build defaults.
   The platform publisher owns this selection and alignment; the selected code remains owned by its respective maintainer.
3. **Build input to augmentation:** application classes, resources, descriptors, build configuration, and deployment artifacts are consumed by build steps, recorders, and bytecode transformation.
4. **Generated deployment output to the operator:** generated configuration, container definitions, Kubernetes resources, and other deployment descriptors can carry build input into an operator-controlled environment.
5. **Build output to runtime:** generated classes and recorded or static-init state become a packaged JVM application or native executable, then runtime initialization opens runtime services.
6. **Runtime ingress:** requests, messages, events, files, and stored or remote data pass through protocol, codec, validation, and integration code to the application.
7. **Management and control plane:** management endpoints, health and observability endpoints, generated administration surfaces, and operator actions can have different exposure and privileges from application endpoints.
8. **Identity, tenant, and object authorization:** credentials pass through an authentication mechanism and identity provider to a `SecurityIdentity`, which is then used for authorization.
   Identity, tenant, object, delegation, and token-audience context must survive that flow. *(documented for the authentication flow: `docs/src/main/asciidoc/security-architecture.adoc`)*
9. **Runtime egress:** Quarkus and application code use operator-supplied configuration and credentials to access databases, brokers, identity providers, cloud services, filesystems, and the operating system.
   Attacker-controlled runtime input can sometimes influence the destination or action, creating a separate confused-deputy or request-forgery boundary.
10. **Logs, telemetry, and error output:** application or attacker-controlled data crosses into logs, traces, metrics, error pages, and management systems, which can expose secrets or affect downstream consumers.
11. **Developer environment:** a browser, terminal, IDE, or remote-dev client reaches live reload, Dev UI, workspace operations, containers, test services, or other external services.
12. **Native and operating-system integration:** native substitutions, FFI/JNI, platform libraries, files, processes, and operating-system APIs can change reachability and impact for a packaging variant.
13. **Ownership and routing:** a Quarkus integration can cross into an upstream library or a community extension.
    The location of the final sink does not by itself decide who owns the defect.

## Claimed security properties

This draft starts with a deliberately short list.
Component documentation can define more specific properties, but a triage decision should cite the applicable model entry or authoritative component documentation.

| Property | Conditions | A possible violation |
| --- | --- | --- |
| Configured authentication and authorization are enforced | A supported Quarkus security mechanism and authorization rule are enabled | Attacker-controlled credentials or request state produce the wrong identity, bypass the configured rule, cross a tenant/object boundary, or disclose credential material *(documented architecture: `docs/src/main/asciidoc/security-architecture.adoc`)* |
| Development-only facilities are not production facilities | A supported packaged production application is used | A Quarkus dev-only route, service, build operation, or Dev UI capability is included and reachable in that production application *(documented mode separation: `docs/src/main/asciidoc/dev-mode-differences.adoc`, `docs/src/main/asciidoc/dev-services.adoc`)* |
| Quarkus-owned security behavior matches its documented contract | The affected component and configuration are supported | Quarkus fails open, creates a broader identity or capability than documented, exposes protected data, or crosses a documented boundary |

A reasonable but undocumented security expectation is not grounds for rejecting a report.
It should be treated as **Needs evidence** or **Model gap** until maintainers decide whether Quarkus claims the property.

## Responsibilities

The following split is proposed as a triage baseline. *(inferred)* [Q5]

- **Quarkus** is responsible for faithfully implementing documented framework behavior and security controls, for Quarkus-owned defaults and generated output, and for integration behavior under supported configurations.
- **Application developers** are responsible for application business authorization, selecting and configuring the security policy their application needs, validating business data, and not deliberately implementing unsafe behavior.
- **Build and runtime operators** are responsible for trusted build inputs, network exposure, supported deployment modes, transport and proxy configuration, secrets, credentials, and appropriate resource limits.
- **Core-repository extension, community extension, and upstream maintainers** are responsible for their own code according to its ownership and support boundary.
  A Quarkus-specific integration or unsafe Quarkus default can still be a Quarkus issue even when an upstream component appears in the path.
- **The publisher of a Quarkus Platform release** is responsible for the version constraints, descriptors, platform defaults, and compatibility promise in that release.
  This responsibility concerns the platform's selection and integration of an extension; it does not transfer ownership of externally maintained extension code.

These are shared boundaries.
Deployment responsibility does not discharge a Quarkus-owned security invariant, misleading or unsafe default, broken enforcement, or failure to safely handle attacker-controlled input.
Routing an issue upstream likewise does not end the Quarkus assessment when its integration, platform selection, default, or exposure materially contributes.

Examples, tests, quickstarts, and documentation snippets do not by themselves establish a production vulnerability.
Official tooling that generates unsafe production code or configuration may still create a valid Quarkus issue. *(inferred)* [Q6]

## Information that helps assess a report

A useful report connects the attacker to the claimed impact.
Include what you can from this list, but do not delay an initial private report to satisfy it:

1. the affected Quarkus component and version;
2. the supported mode, packaging variant, extension and resolved version, imported platform BOMs, any version override, and relevant configuration;
3. the attacker's starting privileges and the input or state they control;
4. the actual path by which that input reaches the affected Quarkus code;
5. the documented or maintainer-confirmed security property that is broken, or the reasonable security expectation that may expose a model gap;
6. the resulting confidentiality, integrity, authorization, or availability impact; and
7. a safe reproducer, source-level analysis, trace, or other evidence that demonstrates the path.

A weaponized exploit is not required.
Evidence can be incomplete, especially in an initial private report.
Maintainers may ask for more information when reachability or impact is unclear.

The following do not establish a vulnerability on their own:

- directly invoking an internal method that no supported application path reaches;
- a test fixture, mock, synthetic class path, or manually corrupted internal state without an in-scope way to create that state;
- using dev mode as a production deployment, where the claimed impact depends on that unsupported use;
- reproducing an effect the same security principal can already cause through an equivalent authorized path, provided there is no new tenant or object access, delegation or audience change, interaction or reauthentication bypass, audit or control bypass, or other incremental security impact;
- an application deliberately omitting or weakening its own authorization;
- deliberate behavior by selected code already executing with granted build or runtime privileges, when the report shows no less-privileged path, Quarkus parsing or selection flaw, confused-deputy behavior, or claimed isolation boundary; or
- a theoretical parser, native, or upstream-library issue without showing that the affected code is present and reachable in the claimed Quarkus variant.

Supported non-default configuration is not automatically out of scope.
An explicitly development-only mode does not establish a production claim, but can still be assessed against the proposed security properties of the developer-tooling boundary.
An unsupported configuration is outside only the property or mode for which support is claimed. *(inferred)* [Q8]

Resource exhaustion needs the same end-to-end account: attacker cost, amplification or unbounded behavior, affected resource, configured limits, and service impact.
A large input consuming a proportionally large amount of work is not necessarily a security vulnerability.

## Triage outcomes

The model is intended to improve routing, not to force every report into “vulnerability” or “invalid.”
The outcomes are not all mutually exclusive: **Needs evidence** is an interim state, and upstream coordination can happen alongside a Quarkus disposition.

- **Vulnerability:** an in-scope, attacker-reachable violation with security impact, handled under the private security process.
  Severity is assessed separately.
- **Needs evidence:** the claim may be valid, but reachability, attacker control, affected mode, property, or impact has not yet been established.
  Missing evidence does not by itself make the claim invalid or public.
- **Hardening:** no issue requiring confidential or coordinated handling is established, but a safer default, clearer documentation, additional validation, or defense in depth would be useful.
  This can move to public handling after the Quarkus security handlers conclude that confidential handling is no longer needed and coordinate with the reporter or an upstream maintainer where applicable.
- **Ordinary bug:** the report identifies a correctness defect without a security impact.
  It can move to the normal issue process after the same confidentiality decision.
- **Upstream or extension routing:** the issue is owned primarily by another project or maintainer.
  Quarkus may coordinate disclosure as described in `SECURITY.md`.
- **By design / application or operator responsibility:** the behavior follows applicable Quarkus documentation or an accepted responsibility boundary and does not violate a Quarkus security property.
- **Previously assessed non-finding:** the report matches a current, linked project analysis with the same assumptions.
  Different reachability, configuration, or impact should trigger a fresh assessment rather than an automatic dismissal.
- **Model gap:** the report exposes a component, actor, mode, or property that this model does not handle.
  The model should then be corrected rather than used to dismiss the report.

## Proposed non-goals and assumptions

The following are proposed for maintainer confirmation; this draft cannot establish project-wide disclaimers on its own:

- that build-time or runtime class loaders sandbox mutually hostile code;
- that Quarkus can protect a process from malicious application code, extensions, build plugins, dependencies, or a privileged operator;
- that catalog or Quarkus Platform membership makes extension code Quarkus-owned, security-audited, or covered by a particular support offering;
- that JVM, AOT, and native implementations have identical low-level attack surfaces; or
- that every crash, exception, large allocation, insecure application configuration, or unsafe upstream API is a Quarkus vulnerability.

Separately, existing documentation explicitly says that dev and remote-dev applications are not production deployments. *(documented: `docs/src/main/asciidoc/dev-mode-differences.adoc`, `docs/src/main/asciidoc/maven-tooling.adoc`)*

## Maintenance

This model should change when Quarkus adds a component family, creates or removes a trust boundary, changes a supported mode, makes a new security guarantee, changes what platform inclusion means for security responsibility, or learns from a report that the current categories are wrong.
Individual extension details belong in focused documents only when the family model cannot express their security boundary.

The model should be reviewed together with `SECURITY.md`.
The website copy of the security policy is maintained separately.
Synchronizing it is a merge condition for an accepted `SECURITY.md` change unless maintainers explicitly choose a different rollout.

Acceptance should also name the maintainer or team responsible for reviewing future model changes.
A model without an owner will eventually become another stale inventory.

## Open maintainer questions

1. **[Q1] Capabilities and inputs:** Does the actor-capability matrix draw the right line between code deliberately granted execution privileges and metadata or content that Quarkus must still parse safely?
   Which Quarkus tools, if any, deliberately accept hostile projects or artifacts?
2. **[Q2] Isolation:** Is deployment/runtime class-loader separation purely an architectural boundary, with no security isolation guarantee?
3. **[Q3] Developer posture:** Which properties do Dev UI, Dev Services, local dev, and remote dev promise for workspaces, credentials, developer hosts, and generated production output?
4. **[Q4] Execution variants:** Should JVM, legacy-jar, AOT, and native claim the same high-level security properties, subject to variant-specific reachability?
5. **[Q5] Extensions and platform responsibility:** Should the generic extension fallback be used as proposed?
   Does the application/operator/ extension-maintainer/Quarkus split match how the project wants to triage extension issues?
   For an external extension included in an official `io.quarkus.platform` release, which version-selection, testing, support, remediation, and disclosure responsibilities does the platform publisher accept beyond the documented compatibility promise?
6. **[Q6] Generated and example code:** When should codestarts, generated configuration, quickstarts, examples, tests, and documentation snippets count as part of the supported security surface?
7. **[Q7] Independent projects:** Are independently consumable projects in this repository covered when used standalone, or only when reached through a Quarkus application?
8. **[Q8] Configuration and availability:** Are supported non-default configurations in scope as proposed, and what amplification or impact threshold should distinguish availability vulnerabilities from ordinary resource use?
9. **[Q9] Project infrastructure:** Should Quarkus CI, release credentials, artifact signing, and publication infrastructure be covered here or in a separate operational threat model?
