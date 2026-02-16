package io.forest.springai;

record ChunkingMetrics(
    double precision,
    double recall,
    double f1Score,
    double mrr,
    double avgRelevancyScore
) {}