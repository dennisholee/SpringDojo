package io.forest.cdm.shared;

/**
 * Base type for domain failures a caller can act on.
 *
 * <p>Carries a stable {@code errorCode} so the web layer can translate <em>every</em> domain failure
 * through a single handler without knowing each subclass - and, importantly, without domain modules
 * importing HTTP types or knowing about status codes.
 */
public abstract class CdmDomainException extends RuntimeException {

    private final String errorCode;

    protected CdmDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Stable, machine-readable identifier for this failure (for example {@code PARTY_NOT_FOUND}). */
    public String errorCode() {
        return errorCode;
    }
}
