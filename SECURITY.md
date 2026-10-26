<!-- content also visible in quarkusio/security
     copy changes there too -->

The canonical version of this document is hosted on the Quarkus website at [Quarkus security](https://quarkus.io/security/).

# Security and Vulnerability Disclosure Policies

The Quarkus team and community take security very seriously.
We want to know when a security bug is found, so that it can be fixed and disclosed as quickly as possible.
This document explains what we consider a security issue, how to report one, what you can expect from us, and how we assign CVE identifiers.

## Reporting security issues

:warning: Do NOT report security issues in our public bug tracker, in GitHub discussions, on Zulip, or in a public pull request.

Please report any security issues you find to:

    security at quarkus.io

Anyone can post to this list.
Its subscribers are trusted individuals from the Quarkus community who handle reported issues in confidence.
Currently, the security response teams of the following distributions are also subscribed and will respond to your report:

* [Red Hat](https://access.redhat.com/security/team/contact/)

You do not need to sign anything to report an issue to us.
We do not require a non-disclosure agreement, and reporting does not depend on accepting any terms and conditions.

If you are not sure whether something is a security issue, report it privately anyway.
We would rather triage a borderline report than see it disclosed publicly by mistake.

### What to include in your report

A good report lets us confirm the issue quickly.
Please include:

1. **The affected versions.** An exact Quarkus version (for example `3.27.1`), or a commit ID for unreleased code. "The latest version" is not enough.
2. **The affected component.** The Quarkus extension, Quarkiverse extension, or SmallRye project, and the module, class or endpoint when you know it.
3. **A description of the problem** and the conditions needed to trigger it: configuration, enabled extensions, launch mode (production, dev mode, test), JVM or native, operating system and platform when relevant.
4. **A working reproducer.** We do not assess a report without one. See [Reproducer requirement](#reproducer-requirement).
5. **The impact** you observed, and how an attacker could exploit it. Please stick to what you were able to demonstrate.

These are welcome but optional:

* The suspected root cause (files, methods).
* A proposed fix or a mitigation. Please send it with the report, not as a public pull request.
* How you would like to be credited, and any embargo or disclosure date you need.

Please send separate reports for unrelated issues, and stay available to answer our questions and test fixes.
If we cannot run your reproducer, or it does not show the issue, we tell you.
We close the report if you do not respond within 14 days.

### Reproducer requirement

We start assessing a report only once it includes a reproducer that demonstrates the issue.
Without one, we ask for it and do not treat the report as a vulnerability.
If no reproducer arrives within 14 days, we close the report.

A valid reproducer:

* **Runs on a specific version.** It targets the Quarkus version stated in the report. We prioritize issues that reproduce on a supported version or a recent commit on `main`. An issue that only reproduces on an End-of-Life version still gets a CVE, but is unlikely to be fixed.
* **Is source code.** A minimal Quarkus application, a test case, or a script, sent as a private Git repository, a zip of the sources, or a patch. We do not run binaries, container images, or obfuscated code.
* **States its configuration.** It uses the default configuration, or lists every non-default property, extension, and environment setting it needs.
* **Comes with exact steps.** The commands, requests, or payloads that trigger the issue, plus the result you observed (logs, stack trace, response) and the result you expected.
* **Shows the impact you claim.** If you report remote code execution, the reproducer executes code. If you report a denial of service, it exhausts the resource with a realistic amount of traffic. If you report an authorization bypass, it reaches the protected resource.

The following are not reproducers: a description of a code path, a static analysis or scanner finding, a fuzzer crash with no input that triggers it, or a theoretical attack scenario.

If you cannot share a reproducer, for example because the issue needs infrastructure you cannot give us, say so in the report and explain how you confirmed the issue. We decide case by case whether to go ahead.

### Reports produced with AI tools

We welcome reports from researchers who use AI tools to find bugs.
We also receive a growing number of AI-generated reports that are long, speculative about impact, or describe issues that do not exist.
Triaging them takes time away from real vulnerabilities.
If you use AI to find or write up an issue, you must:

* **Run the reproducer yourself** against a specific Quarkus version before you send the report. We close AI-assisted reports whose reproducer does not run or does not show the issue, without further analysis.
* **Say that AI tools were used**, and for which part (discovery, analysis, write-up, reproducer, fix).
* **Keep the report short and factual.** Lead with a one-paragraph summary, then the items listed above. Please do not include speculative severity scores or attack scenarios you have not demonstrated.
* **Test any fix you propose.** It must compile and pass the existing tests.
* **Stay available.** A human must be able to answer our questions.

We may close reports that do not follow these rules without further analysis.

Using AI tools does not change how we handle a report: it stays confidential and follows the same coordinated disclosure process as any other report.

## What qualifies as a security issue

A security issue is a bug that an attacker can exploit to harm the confidentiality, integrity, or availability of a Quarkus application, or to bypass a security policy that Quarkus implements or documents.
Examples include:

* Remote code execution, injection, deserialization, or path traversal.
* Authentication or authorization bypass, including in OIDC, JWT, security annotations, or HTTP permission policies.
* Leakage of secrets, credentials, tokens, or other users' data.
* **Denial of service caused by resource exhaustion that an attacker can trigger with a reasonable amount of traffic.** This includes `OutOfMemoryError`, memory leaks, thread or connection exhaustion, unbounded buffering, excessive CPU use (for example, catastrophic regular expressions or hash collisions), and missing limits on request size, header count, or decompression. A memory leak that a remote client can trigger is a security issue, not just a performance bug.
* Insecure default configuration, meaning a default that is dangerous, surprising, or not documented as insecure.
* Development-time features (Dev UI, Dev Services, dev mode, the remote dev mode) that someone other than the developer can reach or abuse, for example from a malicious web page or another machine on the network.

The following are generally **not** considered security issues in Quarkus:

* Vulnerabilities in a third-party dependency (Netty, Vert.x, Hibernate, Jackson, etc.) that Quarkus does not make exploitable. Please report them to the upstream project (see [Ecosystem](#ecosystem)). Updating a dependency to fix an upstream CVE does not create a new Quarkus CVE.
* Problems that only occur after an authorized user explicitly configures Quarkus in a documented, insecure way, for example by disabling TLS verification or exposing dev mode publicly.
* Volumetric denial of service (flooding a service with traffic) when Quarkus provides and enables reasonable limits.
* Reports without a working reproducer (see [Reproducer requirement](#reproducer-requirement)), including scanner output that only lists a dependency version.
* Issues that require physical access to the host, or prior full control of the application or its configuration.
* Code that is deliberately insecure, such as test fixtures, examples, and quickstarts that are not meant to run in production.

When in doubt, report it privately.

## Scope

This policy, and our [CVE Numbering Authority scope](#cve-assignment), covers:

* All repositories of the [quarkusio](https://github.com/quarkusio) GitHub organization.
* All repositories of the [quarkiverse](https://github.com/quarkiverse) GitHub organization. Quarkiverse extensions are maintained by their own communities, and we coordinate the fix with their maintainers.
* The following [SmallRye](https://github.com/smallrye) projects used by Quarkus:
    * [Jandex](https://github.com/smallrye/jandex)
    * [SmallRye Certificate Generator](https://github.com/smallrye/smallrye-certificate-generator)
    * [SmallRye Common](https://github.com/smallrye/smallrye-common)
    * [SmallRye Config](https://github.com/smallrye/smallrye-config)
    * [SmallRye Context Propagation](https://github.com/smallrye/smallrye-context-propagation)
    * [SmallRye Fault Tolerance](https://github.com/smallrye/smallrye-fault-tolerance)
    * [SmallRye GraphQL](https://github.com/smallrye/smallrye-graphql)
    * [SmallRye Health](https://github.com/smallrye/smallrye-health)
    * [SmallRye JWT](https://github.com/smallrye/smallrye-jwt)
    * [SmallRye Mutiny](https://github.com/smallrye/smallrye-mutiny)
    * [SmallRye Mutiny Vert.x bindings](https://github.com/smallrye/smallrye-mutiny-vertx-bindings)
    * [SmallRye Mutiny Zero](https://github.com/smallrye/smallrye-mutiny-zero)
    * [SmallRye OpenAPI](https://github.com/smallrye/smallrye-open-api)
    * [SmallRye OpenSSL](https://github.com/smallrye/smallrye-openssl)
    * [SmallRye Reactive Messaging](https://github.com/smallrye/smallrye-reactive-messaging)
    * [SmallRye Reactive Utilities](https://github.com/smallrye/smallrye-reactive-utils)
    * [SmallRye Stork](https://github.com/smallrye/smallrye-stork)

Out of scope: third-party libraries not listed above, applications built with Quarkus, and vulnerabilities that another CVE Numbering Authority covers more specifically.

### Ecosystem

Quarkus is an ecosystem made of many extensions and libraries (like Eclipse Vert.x, Netty, Hibernate, Apache Camel and more), most of which are not under the direct responsibility of the Quarkus team.
If you find a security bug that may be rooted in one of these libraries, you should report it to them directly.

## What to expect

We acknowledge every report within 72 hours.
We do not commit to a deadline for the assessment or the fix: they depend on the complexity of the issue and on the projects involved.
We keep you informed while we work on it.

Once we confirm the issue, we:

1. Keep you informed of progress, and involve you in reviewing the fix when you want to.
2. Coordinate with the maintainers of affected Quarkiverse extensions, SmallRye projects, or upstream libraries, and with downstream distributions subscribed to the security list.
3. Agree on a disclosure date with you. We prefer short embargoes, and we will not ask you to wait longer than needed to ship a fix in the supported versions.
4. Release the fix, publish a security advisory, and publish the CVE Record.

Embargoed information stays confidential until the agreed disclosure date.
If the issue becomes public before that date, for example through a public commit, a public discussion, or another disclosure, we disclose it immediately, with or without a fix.

We credit reporters in the advisory and in the CVE Record by default, using the name you give us.
Tell us if you prefer to stay anonymous.
Quarkus does not run a bug bounty program.

## CVE assignment

Quarkus is becoming a [CVE Numbering Authority (CNA)](https://www.cve.org/ProgramOrganization/CNAs) and assigns CVE IDs for vulnerabilities in the [scope](#scope) above.
Currently, it relies on Red Hat CNA to get the CVE IDs, but we are working to become a CNA ourselves.
We follow the [CVE Program's CNA Operational Rules](https://www.cve.org/ResourcesSupport/AllResources/CNARules). In practice:

* We assign a CVE ID to every confirmed vulnerability in scope that is or will be publicly disclosed, whether or not a fix exists.
* We give the CVE ID to the reporter.
* We do not assign a new CVE when Quarkus updates a dependency to fix that dependency's CVE. Our advisory references the upstream CVE instead.
* When a vulnerability is in an upstream project that is itself a CNA, we notify it and let it assign the CVE.
* Vulnerabilities in End-of-Life versions get a CVE when they are real. If no supported version is affected, the CVE Record is tagged `unsupported-when-assigned`.
* We publish the CVE Record within 24 hours of public disclosure, and always within 72 hours.
* We publish our security advisories at <!-- TODO: advisory location to be set up --> [TBD](https://quarkus.io/advisories/), and each advisory references its CVE IDs.

If you disagree with one of our decisions (whether something is a vulnerability, a CVE assignment, or the content of a CVE Record), tell us first at security at quarkus.io.
If we cannot agree, the CVE Program's Policy and Procedure for Disputing a CVE Record applies ([CNA Rules, section 4.6](https://www.cve.org/resourcessupport/allresources/cnarules#section_4-6_Dispute_Resolution)).

## Supported versions

The community fixes security bugs in the latest _major.minor_ version and supported LTS versions.

You can find the list of supported versions on the [LTS release list](https://quarkus.io/releases/).

Depending on the severity of the issue and the age of the release, we may also fix older versions, but we only commit to the versions documented on the website and wiki.
