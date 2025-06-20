package com.example.datarouter.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DynamicChannelRegistryTest {

    DynamicChannelRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DynamicChannelRegistry();
    }

    @Nested
    @DisplayName("getOrCreateChannel")
    class GetOrCreateChannelTest {

        @Test
        void testGetOrCreateChannel() {

            // Test creating a new channel
            var channel1 = registry.getOrCreateChannel("testChannel");
            assert channel1 != null;

            // Test retrieving the same channel
            var channel2 = registry.getOrCreateChannel("testChannel");
            assert channel1 == channel2; // Should return the same instance

            // Test existence check
            assert registry.exists("testChannel");

            // Test getting a non-existing channel
            assert registry.getChannel("nonExistingChannel") == null;
        }
    }
}