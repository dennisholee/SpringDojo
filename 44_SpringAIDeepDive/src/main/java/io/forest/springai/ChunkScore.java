package io.forest.springai;

public record ChunkScore(int chunkIndex,
                         double relevancyScore,
                         boolean isRelevant,
                         int rank) {
}
