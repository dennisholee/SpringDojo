package io.forest.opa;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Base64;
import java.nio.ByteBuffer;

public class JwkKeyCreator {

    public static PublicKey createPublicKeyFromJwk(String xBase64Url, String yBase64Url, String curveName) throws Exception {

        // 1. Decode the Base64Url components
        byte[] xBytes = base64UrlDecode(xBase64Url);
        byte[] yBytes = base64UrlDecode(yBase64Url);

        // Ensure leading zero if necessary for BigInteger conversion
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        // 2. Get the standard EC parameter specifications for P-256
        // Java uses the standard name "secp256r1" for "P-256"
        ECParameterSpec ecSpec = getECParameterSpec(curveName);

        // 3. Create the public key specification
        ECPoint ecPoint = new ECPoint(x, y);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(ecPoint, ecSpec);

        // 4. Generate the PublicKey object
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(pubKeySpec);
    }

    /**
     * Helper to decode Base64Url strings used in JWK.
     */
    private static byte[] base64UrlDecode(String input) {
        // Base64Url decoder handles the lack of padding and URL-safe characters
        return Base64.getUrlDecoder().decode(input);
    }

    /**
     * Helper to retrieve the standard ECParameterSpec.
     * In a real application, you might cache this or use a library like Bouncy Castle
     * for broader curve support.
     */
    private static ECParameterSpec getECParameterSpec(String curveName) throws Exception {
        if ("P-256".equals(curveName) || "secp256r1".equals(curveName)) {
            // Using a KeyFactory to generate an EC key pair just to extract the standard spec
            // is a common way to get the built-in parameters.
            java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            return ((java.security.interfaces.ECPublicKey) kpg.generateKeyPair().getPublic()).getParams();
        } else {
            throw new IllegalArgumentException("Unsupported curve: " + curveName);
        }
    }

    // --- Example Usage ---
    public static void main(String[] args) {
        String jwk_x = "WzHfpkzuUn3V9XOdjMlcjSALrHzuLebJ5_N_RsytjjU";
        String jwk_y = "EJlVU5xAkSzEhPdKQW508Ts09zGvOe5hpovHqJ45TZE";
        String jwk_crv = "P-256";

        try {
            PublicKey publicKey = createPublicKeyFromJwk(jwk_x, jwk_y, jwk_crv);
            System.out.println("Public Key Generated Successfully.");
            System.out.println("Algorithm: " + publicKey.getAlgorithm());
            System.out.println("Format: " + publicKey.getFormat());
            // This prints the raw encoded bytes (ASN.1 DER encoding)
            // System.out.println("Encoded bytes (HEX): " + bytesToHex(publicKey.getEncoded()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}