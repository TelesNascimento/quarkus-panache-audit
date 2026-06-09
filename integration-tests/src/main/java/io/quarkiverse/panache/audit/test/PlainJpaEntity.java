package io.quarkiverse.panache.audit.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkiverse.panache.audit.runtime.CreatedBy;
import io.quarkiverse.panache.audit.runtime.LastModifiedBy;

@Entity
@Table(name = "plain_jpa_entity")
public class PlainJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @CreatedBy
    @Column(name = "created_by")
    public String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    public String lastModifiedBy;
}
