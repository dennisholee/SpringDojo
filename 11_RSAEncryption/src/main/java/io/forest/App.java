package io.forest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class App {

	private static final int KEY_LENGTH = 2048;
	private static final String ALGORITHM = "RSA";

	private KeyPair loadKeyPair() throws NoSuchAlgorithmException {
		SecureRandom secureRandom = new SecureRandom();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);

		keyPairGenerator.initialize(KEY_LENGTH, secureRandom);

		return keyPairGenerator.generateKeyPair();
	}

	private String certToString(PublicKey publicKey) {

		throw new NoSuchMethodError("Pending implementation");
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException,
			FileNotFoundException, IOException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, SignatureException {

		App app = new App();

		KeyPair keyPair = app.loadKeyPair();

		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		try (FileOutputStream fos = new FileOutputStream("public.key")) {
			fos.write(publicKey.getEncoded());
		}

		System.out.println(app.certToString(publicKey));

		Cipher encryptCipher = Cipher.getInstance(ALGORITHM);
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

		String secretMessage = "Hello World.";
		byte[] secretMessageBytes = secretMessage.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);

		String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);

		Cipher decryptCipher = Cipher.getInstance(ALGORITHM);
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

		byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);
		String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);

		System.out.println(decryptedMessage);

		Signature signer = Signature.getInstance("SHA256withRSA");
		signer.initSign(privateKey);
		signer.update(encryptedMessageBytes);
		byte[] signature = signer.sign();

		String encodedSignature = Base64.getEncoder().encodeToString(signature);
		System.out.println(encodedSignature);

		signer.initVerify(publicKey);
		signer.update(encryptedMessageBytes);
		boolean verify = signer.verify(signature);
		System.out.println(verify);

	}
}
