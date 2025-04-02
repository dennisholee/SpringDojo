package io.forest.caffeine.requestcoalesce.port;

import io.forest.caffeine.requestcoalesce.domain.User;
import java.util.concurrent.CompletableFuture;

public interface UserPort {
    /**
     * Retrieves a user by their ID.
     * @param userId The unique identifier of the user
     * @return A CompletableFuture containing the user if found
     */
    CompletableFuture<User> getUser(String userId);

    /**
     * Invalidates the cache for a specific user.
     * @param userId The unique identifier of the user
     */
    void invalidateCache(String userId);

    /**
     * Clears the entire cache.
     */
    void clearCache();

    /**
     * Retrieves cache statistics.
     * @return A string containing cache statistics
     */
    String getCacheStats();
} 