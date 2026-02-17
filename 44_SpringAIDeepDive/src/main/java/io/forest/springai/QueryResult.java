package io.forest.springai;

import java.util.List;

/**
 * Represents the results for a single query evaluated by the pipeline.
 *
 * @param query the original user query text
 * @param chunkScores the list of per-chunk scores returned by the evaluator
 * @param totalRelevant total number of chunks considered relevant by the search (used for recall calculations)
 */
record QueryResult(String query,
                   List<ChunkScore> chunkScores,
                   int totalRelevant) {

}
