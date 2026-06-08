package io.quarkiverse.panache.audit.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Principal;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;

@DisplayName("DefaultAuditUserProvider")
class DefaultAuditUserProviderTest {

    private DefaultAuditUserProvider provider;
    private Instance<SecurityIdentity> securityIdentityInstance;
    private SecurityIdentity securityIdentity;

    @BeforeEach
    void setUp() throws Exception {
        provider = new DefaultAuditUserProvider();
        securityIdentityInstance = mock(Instance.class);
        securityIdentity = mock(SecurityIdentity.class);

        // Inject mock via reflection (field injection)
        Field field = DefaultAuditUserProvider.class.getDeclaredField("securityIdentity");
        field.setAccessible(true);
        field.set(provider, securityIdentityInstance);
    }

    @Test
    @DisplayName("authenticated user returns principal name")
    void authenticatedUser_returnsPrincipalName() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("alice");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(securityIdentityInstance.isResolvable()).thenReturn(true);
        when(securityIdentityInstance.get()).thenReturn(securityIdentity);

        assertThat(provider.getCurrentUser()).isEqualTo("alice");
    }

    @Test
    @DisplayName("anonymous identity returns 'anonymous'")
    void anonymousIdentity_returnsAnonymous() {
        when(securityIdentity.isAnonymous()).thenReturn(true);
        when(securityIdentityInstance.isResolvable()).thenReturn(true);
        when(securityIdentityInstance.get()).thenReturn(securityIdentity);

        assertThat(provider.getCurrentUser()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("null identity returns 'anonymous'")
    void nullIdentity_returnsAnonymous() {
        when(securityIdentityInstance.isResolvable()).thenReturn(true);
        when(securityIdentityInstance.get()).thenReturn(null);

        assertThat(provider.getCurrentUser()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("security not resolvable returns 'system'")
    void securityNotResolvable_returnsSystem() {
        when(securityIdentityInstance.isResolvable()).thenReturn(false);

        assertThat(provider.getCurrentUser()).isEqualTo("system");
    }

    @Test
    @DisplayName("null Instance returns 'system'")
    void nullInstance_returnsSystem() throws Exception {
        Field field = DefaultAuditUserProvider.class.getDeclaredField("securityIdentity");
        field.setAccessible(true);
        field.set(provider, null);

        assertThat(provider.getCurrentUser()).isEqualTo("system");
    }

    @Test
    @DisplayName("exception during resolution returns 'system'")
    void exception_returnsSystem() {
        when(securityIdentityInstance.isResolvable()).thenReturn(true);
        when(securityIdentityInstance.get()).thenThrow(new IllegalStateException("CDI context not active"));

        assertThat(provider.getCurrentUser()).isEqualTo("system");
    }

    @Test
    @DisplayName("null principal returns 'system'")
    void nullPrincipal_returnsSystem() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(null);
        when(securityIdentityInstance.isResolvable()).thenReturn(true);
        when(securityIdentityInstance.get()).thenReturn(securityIdentity);

        assertThat(provider.getCurrentUser()).isEqualTo("system");
    }

    @Test
    @DisplayName("getCurrentUser never returns null")
    void getCurrentUser_neverReturnsNull() {
        when(securityIdentityInstance.isResolvable()).thenReturn(false);
        assertThat(provider.getCurrentUser()).isNotNull();
    }
}
