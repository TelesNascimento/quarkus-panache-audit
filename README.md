# quarkus-panache-audit

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

`@CreatedBy` and `@LastModifiedBy` for Quarkus. Works with Panache entities, plain JPA `@Entity`, and Quarkus Data. Fills the gap from [Issue #53104](https://github.com/quarkusio/quarkus/issues/53104).

---

Spring Data JPA ships `@CreatedBy` and `@LastModifiedBy` out of the box. Quarkus only has `@CreationTimestamp` and `@UpdateTimestamp` (timestamps only, no user tracking). Every Quarkus project that needs auditing ends up writing the same boilerplate in every service method.

This extension solves it with two annotations backed by Hibernate 6 `BeforeExecutionGenerator`. It works with **any Hibernate-managed entity** - `PanacheEntity`, `PanacheEntityBase`, Quarkus Data entities, or plain `@Entity` classes:

```java
@Entity
public class Order extends PanacheEntity {

    public String product;

    @CreatedBy
    public String createdBy;       // populated on INSERT, never touched again

    @LastModifiedBy
    public String lastModifiedBy;  // updated on every INSERT and UPDATE
}
```

Also works with plain JPA entities - no Panache required:

```java
@Entity
public class Payment {

    @Id @GeneratedValue
    public Long id;

    @CreatedBy
    public String createdBy;

    @LastModifiedBy
    public String lastModifiedBy;
}
```

With any Quarkus Security extension on the classpath (`quarkus-oidc`, `quarkus-smallrye-jwt`, etc.), `createdBy` and `lastModifiedBy` are populated automatically from `SecurityIdentity`.

---

## Installation

```xml
<dependency>
    <groupId>io.quarkiverse.panacheaudit</groupId>
    <artifactId>quarkus-panache-audit</artifactId>
    <version>1.0.0</version>
</dependency>
```

Requires `quarkus-security` or any Quarkus security extension. If you have OIDC or JWT, you already have it.

---

## How it works

`@CreatedBy` uses `EventTypeSets.INSERT_ONLY`. Hibernate will never call the generator during UPDATE events, so the original value is permanent. `@LastModifiedBy` uses `EventTypeSets.INSERT_AND_UPDATE` and is updated on every write.

Both annotations use the correct Hibernate 6+ API (`@ValueGenerationType` + `BeforeExecutionGenerator`). The deprecated `@GeneratorType` + `ValueGenerator` path is not used.

User resolution goes through a CDI interface:

```java
public interface AuditUserProvider {
    String getCurrentUser();
}
```

The default implementation reads from `SecurityIdentity` and returns `"anonymous"` for unauthenticated requests or `"system"` when there is no security context (batch jobs, Kafka consumers). You can override it by declaring your own `@ApplicationScoped` bean. CDI `@DefaultBean` semantics handle the priority automatically.

---

## Custom provider

```java
@ApplicationScoped
public class TenantAuditUserProvider implements AuditUserProvider {

    @Inject
    SecurityIdentity security;

    @Inject
    TenantContext tenant;

    @Override
    public String getCurrentUser() {
        if (security.isAnonymous()) {
            return "system@" + tenant.current();
        }
        return security.getPrincipal().getName() + "@" + tenant.current();
    }
}
```

No configuration needed. Your bean automatically replaces the default.

---

## Testing

```java
@QuarkusTest
class OrderTest {

    @Test
    @TestSecurity(user = "alice")
    @Transactional
    void createdBy_isPopulated_onFirstPersist() {
        Order order = new Order();
        order.product = "Burger";
        order.persistAndFlush();

        assertThat(order.createdBy).isEqualTo("alice");
        assertThat(order.lastModifiedBy).isEqualTo("alice");
    }

    @Test
    @TestSecurity(user = "alice")
    @Transactional
    void createdBy_isNeverOverwritten_onUpdate() {
        Order order = new Order();
        order.product = "Burger";
        order.persistAndFlush();

        order.product = "Pizza";
        entityManager.flush();

        assertThat(order.createdBy).isEqualTo("alice");
    }
}
```

---

## Roadmap

- [x] `@CreatedBy` - INSERT only, never overwritten on UPDATE
- [x] `@LastModifiedBy` - INSERT and UPDATE
- [x] Zero-config via Quarkus SecurityIdentity
- [x] Pluggable via `AuditUserProvider` CDI interface
- [x] GraalVM native image support
- [ ] MongoDB Panache variant (see [Issue #23420](https://github.com/quarkusio/quarkus/issues/23420))
- [ ] Hibernate Reactive support (different thread context, non-trivial)

---

## Limitations

Does not support `quarkus-hibernate-reactive-panache`. Hibernate Reactive requires async generators (`ReactiveIdentifierGenerator`) while this extension uses the synchronous `BeforeExecutionGenerator`. They are not compatible.

---

## License

Apache License 2.0 - see [LICENSE](LICENSE).
