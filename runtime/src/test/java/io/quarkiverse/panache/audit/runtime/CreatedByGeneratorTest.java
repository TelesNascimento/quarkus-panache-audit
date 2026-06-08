package io.quarkiverse.panache.audit.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

@DisplayName("CreatedByGenerator")
class CreatedByGeneratorTest {

    private final CreatedByGenerator generator = new CreatedByGenerator();

    @Test
    @DisplayName("getEventTypes returns INSERT_ONLY")
    void getEventTypes_returnsInsertOnly() {
        EnumSet<EventType> types = generator.getEventTypes();
        assertThat(types).containsExactly(EventType.INSERT);
        assertThat(types).doesNotContain(EventType.UPDATE);
    }

    @Test
    @DisplayName("getEventTypes() equals EventTypeSets.INSERT_ONLY")
    void getEventTypes_equalsInsertOnly() {
        assertThat(generator.getEventTypes()).isEqualTo(EventTypeSets.INSERT_ONLY);
    }

    @Test
    @DisplayName("generate() never returns null (CDI unavailable -> 'unknown')")
    void generate_neverReturnsNull() {
        Object result = generator.generate(null, null, null, EventType.INSERT);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("generate() returns 'unknown' when CDI container is not running")
    void generate_withoutCdi_returnsUnknown() {
        Object result = generator.generate(null, null, null, EventType.INSERT);
        assertThat(result.toString()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("generate() does not throw when called with null parameters")
    void generate_withNullParameters_doesNotThrow() {
        assertThat(generator.generate(null, null, null, EventType.INSERT))
                .isNotNull();
    }

    @Test
    @DisplayName("generate() returns 'unknown' when handle is not available")
    @SuppressWarnings("unchecked")
    void generate_whenHandleNotAvailable_returnsUnknown() {
        try (var arcMock = mockStatic(Arc.class)) {
            ArcContainer container = mock(ArcContainer.class);
            InstanceHandle<AuditUserProvider> handle = mock(InstanceHandle.class);
            arcMock.when(Arc::container).thenReturn(container);
            when(container.instance(AuditUserProvider.class)).thenReturn(handle);
            when(handle.isAvailable()).thenReturn(false);

            assertThat(generator.generate(null, null, null, EventType.INSERT).toString())
                    .isEqualTo("unknown");
        }
    }

    @Test
    @DisplayName("generate() returns user when provider returns valid name")
    @SuppressWarnings("unchecked")
    void generate_whenProviderReturnsUser_returnsUser() {
        try (var arcMock = mockStatic(Arc.class)) {
            ArcContainer container = mock(ArcContainer.class);
            InstanceHandle<AuditUserProvider> handle = mock(InstanceHandle.class);
            AuditUserProvider provider = mock(AuditUserProvider.class);
            arcMock.when(Arc::container).thenReturn(container);
            when(container.instance(AuditUserProvider.class)).thenReturn(handle);
            when(handle.isAvailable()).thenReturn(true);
            when(handle.get()).thenReturn(provider);
            when(provider.getCurrentUser()).thenReturn("alice");

            assertThat(generator.generate(null, null, null, EventType.INSERT).toString())
                    .isEqualTo("alice");
        }
    }

    @Test
    @DisplayName("generate() returns 'unknown' when provider returns null")
    @SuppressWarnings("unchecked")
    void generate_whenProviderReturnsNull_returnsUnknown() {
        try (var arcMock = mockStatic(Arc.class)) {
            ArcContainer container = mock(ArcContainer.class);
            InstanceHandle<AuditUserProvider> handle = mock(InstanceHandle.class);
            AuditUserProvider provider = mock(AuditUserProvider.class);
            arcMock.when(Arc::container).thenReturn(container);
            when(container.instance(AuditUserProvider.class)).thenReturn(handle);
            when(handle.isAvailable()).thenReturn(true);
            when(handle.get()).thenReturn(provider);
            when(provider.getCurrentUser()).thenReturn(null);

            assertThat(generator.generate(null, null, null, EventType.INSERT).toString())
                    .isEqualTo("unknown");
        }
    }
}
