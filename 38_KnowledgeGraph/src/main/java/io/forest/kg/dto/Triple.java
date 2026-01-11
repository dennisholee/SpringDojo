package io.forest.kg.dto;

/**
 * Simple immutable representation of a subject-predicate-object triple.
 *
 * <p>Each field is a plain String; callers are responsible for normalizing or
 * sanitizing values before persisting them into Neo4j (e.g., converting the
 * predicate into a valid relationship type).</p>
 *
 * @param subject   the subject of the triple
 * @param predicate the predicate of the triple
 * @param object    the object of the triple
 */
public record Triple(
    String subject,
    String predicate,
    String object) {

}
