package io.forest.kg.dto;

import java.util.List;

/**
 * DTO representing the output of an SPO extraction operation performed by an LLM.
 *
 * <p>Contains a list of {@link Triple} objects. The transform step stores the
 * extracted triples in the Document metadata under the key {@code "triples"}.</p>
 *
 * @see Triple
 */
public record SPOExtraction(
    List<Triple> triples) {

}
