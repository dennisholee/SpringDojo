package io.forest.integrationhub.integrity;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChecksumUtilsTest {

    @Test
    void sha256_of_hello_matches_known_value() {
        String input = "hello";
        String hex = ChecksumUtils.sha256Hex(input);
        // Known SHA-256 for "hello"
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hex);
    }

    @Test
    void crc32_matches_java_crc32() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        CRC32 ref = new CRC32();
        ref.update(data);
        long expected = ref.getValue();
        long actual = ChecksumUtils.crc32(data);
        assertEquals(expected, actual);
    }
}
