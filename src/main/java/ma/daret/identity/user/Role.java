package ma.daret.identity.user;

/**
 * RBAC roles. Authorities are exposed to Spring Security as {@code ROLE_<name>}.
 */
public enum Role {
    MEMBER,
    ADMIN,
    COMPLIANCE_OFFICER
}
