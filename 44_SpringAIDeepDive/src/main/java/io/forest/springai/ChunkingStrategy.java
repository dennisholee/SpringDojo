package io.forest.springai;

/**
 * @param name
 * @param chunkSize The target or maximum length for each chunk, typically measured in characters or tokens.
 * @param minChunkSizeChars A threshold to prevent creating tiny, "noisy" chunks (like single words or punctuation) that lack enough context for a meaningful embedding.
 * @param minChunkLengthToEmbed Similar to min chunk size, this ensures that a segment is only converted into a vector if it contains enough information to be useful for semantic search.
 * @param maxNumChunks A hard limit on the total number of segments generated from a single document to control storage costs or processing time.
 */
record ChunkingStrategy(String name,
                        int chunkSize,
                        int minChunkSizeChars,
                        int minChunkLengthToEmbed,
                        int maxNumChunks)
{}
