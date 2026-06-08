# Changelog

## [1.0.0-SNAPSHOT] - 2026-06

### Added
- `@CreatedBy` annotation: auto-populated on INSERT using Hibernate 6 `BeforeExecutionGenerator` (INSERT_ONLY — never overwritten on UPDATE)
- `@LastModifiedBy` annotation: auto-populated on INSERT and UPDATE
- `AuditUserProvider` CDI interface for pluggable user resolution
- `DefaultAuditUserProvider`: zero-config integration with Quarkus `SecurityIdentity`
- `@DefaultBean` support: override default provider by declaring your own `@ApplicationScoped AuditUserProvider`
- GraalVM native image support via `ReflectiveClassBuildItem`
- 21 tests: 10 @QuarkusTest integration + 5 unit (CreatedByGenerator) + 6 unit (LastModifiedByGenerator)
- Fills gap from Quarkus Issue #53104 (open since 2025)
