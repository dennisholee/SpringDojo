package io.forest.springai;

/**
 * A small value object representing the evaluation of a single chunk
 * against a query by the LLM-based evaluator.
 *
 * @param chunkIndex the original index of the chunk in the source document
 * @param relevancyScore a decimal score (0.0 - 1.0) indicating how relevant the chunk is
 * @param isRelevant boolean flag indicating whether the evaluator considered the chunk relevant
 * @param rank the ranking position returned by the similarity search (1-based)
 * @param sufficiencyScore a decimal (0.0 - 1.0) indicating whether the chunk contains sufficient context
 * @param reason short human-readable reason or feedback from the evaluator
 */
public record ChunkScore(int chunkIndex,
                         float relevancyScore,
                         boolean isRelevant,
                         int rank,
                         float sufficiencyScore,
                         String reason) {
}
