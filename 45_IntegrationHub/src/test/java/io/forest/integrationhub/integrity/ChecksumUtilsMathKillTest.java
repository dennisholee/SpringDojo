package io.forest.integrationhub.integrity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChecksumUtilsMathKillTest {

    @Test
    void bytesToHex_specificVector_killsMathMutant() {
        byte[] data = new byte[] { (byte)0x0f, (byte)0xa0, (byte)0x01, (byte)0xff };
        String hex = ChecksumUtils.sha256Hex(new byte[] { 0 }); // keep existing api usage intact
        // Directly test bytesToHex via reflection since it's private
        try {
            java.lang.reflect.Method m = ChecksumUtils.class.getDeclaredMethod("bytesToHex", byte[].class);
            m.setAccessible(true);
            String result = (String) m.invoke(null, (Object) data);
            assertEquals("0fa001ff", result);
            assertEquals(data.length * 2, result.length());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
