package io.quarkiverse.panache.audit.runtime;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class CreatedByGenerator implements BeforeExecutionGenerator {

    private static final String UNKNOWN = "unknown";

    @Override
    public Object generate(SharedSessionContractImplementor session,
            Object owner,
            Object currentValue,
            EventType eventType) {
        if (Arc.container() == null) {
            return UNKNOWN;
        }
        InstanceHandle<AuditUserProvider> handle = Arc.container().instance(AuditUserProvider.class);
        if (!handle.isAvailable()) {
            return UNKNOWN;
        }
        String user = handle.get().getCurrentUser();
        if (user != null) {
            return user;
        }
        return UNKNOWN;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}
