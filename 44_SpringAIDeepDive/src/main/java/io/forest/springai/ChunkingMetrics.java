package io.forest.springai;

/**
 * Holds aggregated chunking evaluation metrics computed after a pipeline run.
 *
 * @param precision average precision across queries (Precision@K)
 * @param recall average recall across queries
 * @param f1Score average F1-score across queries
 * @param mrr mean reciprocal rank across queries
 * @param avgRelevancyScore average relevancy score returned by the evaluator
 * @param avgSufficiency average sufficiency score returned by the evaluator
 */
record ChunkingMetrics(
    float precision,
    float recall,
    float f1Score,
    float mrr,
    float avgRelevancyScore,
    float avgSufficiency
) {}