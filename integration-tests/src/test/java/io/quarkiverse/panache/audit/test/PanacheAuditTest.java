package io.quarkiverse.panache.audit.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PanacheAuditTest {

    @Inject
    EntityManager em;

    @Test
    @Order(1)
    @TestSecurity(user = "darlysson")
    @Transactional
    void createdBy_isPopulated_onFirstPersist() {
        TestEntity entity = TestEntity.of("Venue QikServe London");
        entity.persistAndFlush();

        assertThat(entity.id).isNotNull();
        assertThat(entity.createdBy)
                .as("@CreatedBy should be populated with the authenticated user on INSERT")
                .isEqualTo("darlysson");
        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy should be populated with the authenticated user on INSERT")
                .isEqualTo("darlysson");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "darlysson")
    @Transactional
    void createdBy_isNeverOverwritten_onUpdate() {
        TestEntity entity = TestEntity.of("Original name");
        entity.persistAndFlush();

        assertThat(entity.createdBy)
                .as("@CreatedBy should be 'darlysson' after initial persist")
                .isEqualTo("darlysson");

        entity.name = "Updated name";
        em.flush();

        assertThat(entity.createdBy)
                .as("@CreatedBy must remain 'darlysson' after UPDATE - it uses INSERT_ONLY semantics")
                .isEqualTo("darlysson");
    }

    @Test
    @Order(3)
    @TestSecurity(user = "darlysson")
    @Transactional
    void lastModifiedBy_isPopulated_onInsert() {
        TestEntity entity = TestEntity.of("Test for lastModifiedBy");
        entity.persistAndFlush();

        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy should be 'darlysson' after INSERT")
                .isEqualTo("darlysson");
    }

    @Test
    @Order(4)
    @Transactional
    void anonymousUser_isHandledGracefully_withoutException() {
        TestEntity entity = TestEntity.of("Anonymous entity");
        entity.persistAndFlush();

        assertThat(entity.id).isNotNull();
        assertThat(entity.createdBy)
                .as("@CreatedBy without auth context must return a non-null, non-blank fallback")
                .isNotNull()
                .isNotBlank();
        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy without auth context must return a non-null, non-blank fallback")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @Order(5)
    @TestSecurity(user = "another-user")
    @Transactional
    void differentUser_isCorrectlyCaptured() {
        TestEntity entity = TestEntity.of("Entity by another-user");
        entity.persistAndFlush();

        assertThat(entity.createdBy)
                .as("@CreatedBy should capture the exact SecurityIdentity principal name")
                .isEqualTo("another-user");
        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy should capture the exact SecurityIdentity principal name")
                .isEqualTo("another-user");
    }

    @Test
    @Order(6)
    @TestSecurity(user = "db-round-trip-user")
    @Transactional
    void auditFields_arePersisted_andRetrievedFromDb() {
        TestEntity entity = TestEntity.of("DB round-trip test");
        entity.persistAndFlush();

        Long id = entity.id;

        em.detach(entity);

        TestEntity fromDb = TestEntity.findById(id);

        assertThat(fromDb).isNotNull();
        assertThat(fromDb.createdBy)
                .as("@CreatedBy must survive the DB round-trip")
                .isEqualTo("db-round-trip-user");
        assertThat(fromDb.lastModifiedBy)
                .as("@LastModifiedBy must survive the DB round-trip")
                .isEqualTo("db-round-trip-user");
    }

    @Test
    @Order(7)
    @TestSecurity(user = "alice")
    @Transactional
    void lastModifiedBy_isSetOnUpdate_insertAndUpdateSemantics() {
        TestEntity entity = TestEntity.of("Will be updated");
        entity.persistAndFlush();
        assertThat(entity.lastModifiedBy).isEqualTo("alice");

        entity.name = "Updated by alice";
        em.flush();

        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy must be re-generated on UPDATE (INSERT_AND_UPDATE semantics)")
                .isEqualTo("alice");
        assertThat(entity.name).isEqualTo("Updated by alice");
    }

    @Test
    @Order(8)
    @TestSecurity(user = "batch-user")
    @Transactional
    void multipleEntitiesInSameTransaction_eachGetsCorrectUser() {
        TestEntity e1 = TestEntity.of("Batch 1");
        TestEntity e2 = TestEntity.of("Batch 2");
        TestEntity e3 = TestEntity.of("Batch 3");

        e1.persistAndFlush();
        e2.persistAndFlush();
        e3.persistAndFlush();

        assertThat(e1.createdBy).isEqualTo("batch-user");
        assertThat(e2.createdBy).isEqualTo("batch-user");
        assertThat(e3.createdBy).isEqualTo("batch-user");
    }

    @Test
    @Order(9)
    @TestSecurity(user = "alice")
    @Transactional
    void createdBy_isPreserved_afterMerge() {
        TestEntity entity = TestEntity.of("Merge test");
        entity.persistAndFlush();
        Long id = entity.id;
        assertThat(entity.createdBy).isEqualTo("alice");

        em.detach(entity);
        entity.name = "After merge";
        em.merge(entity);
        em.flush();

        em.clear();
        TestEntity reloaded = TestEntity.findById(id);
        assertThat(reloaded.createdBy)
                .as("@CreatedBy must not be changed after detach+merge+flush")
                .isEqualTo("alice");
    }

    @Test
    @Order(10)
    @TestSecurity(user = "query-user")
    @Transactional
    void auditFields_canBeQueriedByCreatedBy() {
        TestEntity entity = TestEntity.of("Queryable entity");
        entity.persistAndFlush();

        long count = TestEntity.count("createdBy", "query-user");
        assertThat(count)
                .as("Should find at least the entity just created by 'query-user'")
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(11)
    @TestSecurity(user = "alice")
    @Transactional
    void plainJpaEntity_createdBy_isPopulated_onInsert() {
        PlainJpaEntity entity = new PlainJpaEntity();
        entity.name = "Plain JPA";
        em.persist(entity);
        em.flush();

        assertThat(entity.createdBy)
                .as("@CreatedBy must work on plain @Entity without PanacheEntity")
                .isEqualTo("alice");
        assertThat(entity.lastModifiedBy)
                .as("@LastModifiedBy must work on plain @Entity without PanacheEntity")
                .isEqualTo("alice");
    }

    @Test
    @Order(12)
    @TestSecurity(user = "alice")
    @Transactional
    void plainJpaEntity_createdBy_isNeverOverwritten_onUpdate() {
        PlainJpaEntity entity = new PlainJpaEntity();
        entity.name = "Original";
        em.persist(entity);
        em.flush();
        assertThat(entity.createdBy).isEqualTo("alice");

        entity.name = "Updated";
        em.flush();

        PlainJpaEntity reloaded = em.find(PlainJpaEntity.class, entity.id);
        assertThat(reloaded.createdBy)
                .as("@CreatedBy must not change on UPDATE for plain @Entity")
                .isEqualTo("alice");
    }
}
