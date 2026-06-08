package io.quarkiverse.panache.audit.runtime;

import java.security.Principal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
@DefaultBean
public class DefaultAuditUserProvider implements AuditUserProvider {

    private static final Logger LOG = Logger.getLogger(DefaultAuditUserProvider.class);
    private static final String SYSTEM = "system";
    private static final String ANONYMOUS = "anonymous";

    @Inject
    Instance<SecurityIdentity> securityIdentity;

    @Override
    public String getCurrentUser() {
        try {
            return resolveUser();
        } catch (RuntimeException e) {
            LOG.debugf(e, "Failed to resolve SecurityIdentity for audit user, falling back to '%s'", SYSTEM);
            return SYSTEM;
        }
    }

    private String resolveUser() {
        if (!isSecurityAvailable()) {
            return SYSTEM;
        }
        SecurityIdentity identity = securityIdentity.get();
        if (identity == null || identity.isAnonymous()) {
            return ANONYMOUS;
        }
        Principal principal = identity.getPrincipal();
        if (principal == null) {
            return SYSTEM;
        }
        return principal.getName();
    }

    private boolean isSecurityAvailable() {
        return securityIdentity != null && securityIdentity.isResolvable();
    }
}
