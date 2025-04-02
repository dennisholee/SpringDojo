package io.forest.caffeine.requestcoalesce.adapter;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.forest.caffeine.requestcoalesce.domain.User;
import io.forest.caffeine.requestcoalesce.model.UserData;
import io.forest.caffeine.requestcoalesce.mapper.UserMapper;
import io.forest.caffeine.requestcoalesce.port.UserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedUserAdapter implements UserPort {

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final AsyncLoadingCache<String, CompletableFuture<User>> cache;
    private final String apiUrl;

    public CachedUserAdapter(
            RestTemplate restTemplate,
            UserMapper userMapper,
            @Value("${api.user.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.userMapper = userMapper;
        this.apiUrl = apiUrl;
        this.cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .buildAsync(this::fetchUserFromApi);
    }

    @Override
    public CompletableFuture<User> getUser(String userId) {
        log.debug("Attempting to get user with ID: {}", userId);
        return cache.get(userId).thenCompose(future -> future);
    }

    private CompletableFuture<User> fetchUserFromApi(String userId) {
        log.info("Cache miss for user ID: {}. Fetching from API.", userId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<UserData> response = restTemplate.getForEntity(
                    apiUrl + "/" + userId,
                    UserData.class
                );
                
                if (response.getBody() == null) {
                    throw new RuntimeException("No user data received from API");
                }
                
                return userMapper.toUser(response.getBody());
            } catch (Exception e) {
                log.error("Error fetching user data for ID: {}", userId, e);
                throw new RuntimeException("Failed to fetch user data", e);
            }
        });
    }

    @Override
    public void invalidateCache(String userId) {
        log.info("Invalidating cache for user ID: {}", userId);
        cache.synchronous().invalidate(userId);
    }

    @Override
    public void clearCache() {
        log.info("Clearing entire cache");
        cache.synchronous().invalidateAll();
    }

    @Override
    public String getCacheStats() {
        return cache.synchronous().stats().toString();
    }
} 