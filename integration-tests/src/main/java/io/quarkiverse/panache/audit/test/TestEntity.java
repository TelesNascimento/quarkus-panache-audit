package io.quarkiverse.panache.audit.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkiverse.panache.audit.runtime.CreatedBy;
import io.quarkiverse.panache.audit.runtime.LastModifiedBy;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "test_entity")
public class TestEntity extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @CreatedBy
    @Column(name = "created_by")
    public String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    public String lastModifiedBy;

    public static TestEntity of(String name) {
        TestEntity entity = new TestEntity();
        entity.name = name;
        return entity;
    }
}
