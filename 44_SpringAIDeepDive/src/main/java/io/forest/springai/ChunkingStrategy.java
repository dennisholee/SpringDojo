package io.forest.springai;

/**
 * Configuration describing how to split a document into chunks for embedding
 * and indexing.
 *
 * <p>Typical usage: provide a {@code ChunkingStrategy} to the pipeline so it
 * can decide chunk sizes, minimums, and maximums to control indexing cost
 * and search granularity.</p>
 *
 * @param name a human-readable name for the profile (e.g. "The Specialist")
 * @param chunkSize the target or maximum length for each chunk (characters or tokens)
 * @param minChunkSizeChars minimum characters required to keep a chunk; prevents noisy tiny chunks
 * @param minChunkLengthToEmbed minimum length a chunk must have to be converted into an embedding
 * @param maxNumChunks maximum number of chunks to create from a single document
 */
record ChunkingStrategy(String name,
                        int chunkSize,
                        int minChunkSizeChars,
                        int minChunkLengthToEmbed,
                        int maxNumChunks)
{}
