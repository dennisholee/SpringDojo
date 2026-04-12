# ArchUnit Rules & Remediation Guide

This document defines the concrete architecture rules to enforce hexagonal boundaries in the Integration Hub repository, examples of violations, and suggested remediation steps.

## Naming and package conventions (assumed)

- Core domain packages: `io.forest.integrationhub.core..`  
- Ports: `io.forest.integrationhub.ports..` or `io.forest.integrationhub.api..`  
- Adapters: `io.forest.integrationhub.adapters.*..` (web, messaging, rpc, file, legacy)  

Adjust package names below if your repo uses different conventions.

## Rule set (high-level)

1. **Core must be framework-agnostic** (Fail)

   - Description: Classes in `io.forest.integrationhub.core..` MUST NOT depend on adapter or framework packages (Spring, WebFlux, HTTP clients, Kafka, etc.).
   - ArchUnit (example pseudocode):

```java
// Pseudocode — add to ArchUnit test
classes().that().resideInAPackage("..core..")
  .should().onlyDependOnClassesThat()
  .resideInAnyPackage("..core..", "java..", "javax..", "org.slf4j..", "io.forest.integrationhub.ports..");
```

   - Remediation: Introduce a `Port` interface under `io.forest.integrationhub.ports` and move adapter-specific usage behind an adapter implementation.

2. **Adapters may depend on core** (Enforce)

   - Description: Adapter packages (web, messaging, rpc, file, legacy) MAY depend on core and ports, but not vice versa.

3. **No Spring framework annotations in core** (Fail)

   - Description: Core classes must not carry Spring annotations like `@Component`, `@Autowired`, `@Controller`, etc.
   - Remediation: Create configuration classes in adapters that wire core beans via constructor injection; keep core POJOs annotation-free.

4. **Layering: domain → ports → adapters** (Fail)

   - Description: Enforce that the dependency graph follows domain → ports → adapters and forbid cross-layer violations.

5. **Whitelists for permitted exceptions** (Warn/Allow)

   - Description: Some legacy cases may require documented exceptions. Maintain an allowlist file (`specs/main/archunit-allowlist.md`) with justifications and scope.

## Severity & Remediation Matrix

- **Fail**: Must fix before merging. Examples: core → adapter dependency, Spring config in core.  
- **Warn**: Allowed but requires documented justification in allowlist. Examples: generated sources referencing infra.

## Examples

- Violation: `io.forest.integrationhub.core.payment.PaymentService` imports `org.springframework.web.reactive.function.client.WebClient` (FAIL).  
  Remediation: Extract an outbound `PaymentGateway` port, implement adapter using `WebClient` in `io.forest.integrationhub.adapters.web.payment`, inject into orchestration code.

- Violation: `io.forest.integrationhub.core.model.*` annotated with `@Component` (FAIL).  
  Remediation: Remove annotation; expose factory or configuration in adapters.

## How to add new rules

1. Add a new rule to `src/test/java/.../ArchUnitRulesTest.java` (create tests that assert a failure).  
2. Add pass/fail examples under `src/test/java/io/forest/integrationhub/` to accompany the rule.  
3. Document the rule in this file with examples and remediation steps.

## Developer Remediation Patterns (cheat sheet)

- Introduce `Port` interfaces in `io.forest.integrationhub.ports` for external dependencies.  
- Add mappers in `io.forest.integrationhub.adapters.*.mapper` to translate outbound/inbound DTOs.  
- Use constructor injection in adapters to pass implementations into orchestration components.  

## Allowlist process

- Create `specs/main/archunit-allowlist.md` with one entry per exception, including: file/class, reason, owner, and expiration (review date).
