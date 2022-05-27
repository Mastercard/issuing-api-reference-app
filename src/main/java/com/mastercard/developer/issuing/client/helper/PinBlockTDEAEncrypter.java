/**
 *  Copyright (c) 2022 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastercard.developer.issuing.client.helper;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.mastercard.developer.issuing.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.generated.models.EncryptedPinBlock;

import lombok.extern.log4j.Log4j2;


@Log4j2
public final class PinBlockTDEAEncrypter {

    /** The Constant ENCRYPTION_KEYSTORE_PATH. */
    private static final String PIN_ENCRYPTION_KEYSTORE_PATH = "mi.api.pin.encryption.tdea.public.key.file";

    /** The Constant PIN_ENCRYPTION_TRANSFORMATION. */
    private static final String PIN_ENCRYPTION_TRANSFORMATION = "mi.api.pin.encryption.secret.key.transformation";

    /** The Constant PIN_ENCRYPTION_ALGO. */
    private static final String PIN_ENCRYPTION_ALGO = "mi.api.pin.encryption.secret.key.algorithm";

    /** The Constant KEY_SIZE. Double DES uses 112 bit key block and Triple DES uses 168 bit key block */
    private static final int KEY_SIZE = 168;

    /** The self. */
    private static PinBlockTDEAEncrypter self;

    /** The rsa public key. */
    private PublicKey rsaPublicKey;

    /**
     * Instantiates a new pin block TDEA encrypter.
     *
     * @throws GeneralSecurityException the general security exception
     */
    private PinBlockTDEAEncrypter() throws GeneralSecurityException {
        String pinEncryptionKeystorePath = ApiClientHelper.getProperty(PIN_ENCRYPTION_KEYSTORE_PATH);

        /** Step 5.1: Load RSA public key from file - Loaded during instance creation */
        rsaPublicKey = getRSAPublicKey(pinEncryptionKeystorePath);
    }

    /**
     * Gets the single instance of PinBlockTDEAEncrypter.
     *
     * @return single instance of PinBlockTDEAEncrypter
     * @throws GeneralSecurityException the general security exception
     */
    public static PinBlockTDEAEncrypter getInstance() throws GeneralSecurityException {
        if (self == null) {
            self = new PinBlockTDEAEncrypter();
        }
        return self;
    }

    /**
     * Encrypt pin block.
     *
     * @param desKey   the des key
     * @param pinBlock the pin block
     * @return the string
     * @throws ReferenceAppGenericException the reference app generic exception
     */
    public String encryptPinBlock(String desKey, String pinBlock) throws ReferenceAppGenericException {
        String encryptedPinBlock = null;
        try {
            /** Step 1 - Create Cipher for encryption transformation */
            Cipher cipher = Cipher.getInstance(ApiClientHelper.getProperty(PIN_ENCRYPTION_TRANSFORMATION));

            /** Step 1 - Convert hex encoded 'DES key' to byte array */
            byte[] key = Hex.decodeHex(desKey.toCharArray());

            /** Step 2 - Initializes 'Cipher' with a key. */
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ApiClientHelper.getProperty(PIN_ENCRYPTION_ALGO)));

            /** Step 3 - Convert hex encoded input 'PIN Block' to byte array */
            byte[] pinBlockByteArray = Hex.decodeHex(pinBlock.toCharArray());

            /** Step 4 - Encrypts the 'PIN Block' */
            byte[] cipherText = cipher.doFinal(pinBlockByteArray);

            /** Step 5 - Convert the encrypted 'PIN Block' bytes to HEX string */
            encryptedPinBlock = Hex.encodeHexString(cipherText);

            /** Step 5 - Convert final encrypted 'PIN Block' string to UPPER case string. */
            encryptedPinBlock = encryptedPinBlock.toUpperCase();

        } catch (GeneralSecurityException | DecoderException e) {
            log.error("Exception in encryptPin while encrypting pin block", e);
            throw new ReferenceAppGenericException("Exception while encrypting pin block", e);
        }
        return encryptedPinBlock;
    }

    /**
     * Encrypt pin.
     *
     * @param pin        the pin
     * @param cardNumber the card number
     * @return the encrypted pin block
     * @throws ReferenceAppGenericException the reference app generic exception
     */
    public EncryptedPinBlock encryptPin(char[] pin, String cardNumber) throws ReferenceAppGenericException {

        EncryptedPinBlock encryptedPinBlock = new EncryptedPinBlock();
        /** PIN block ISO-0 (FORMAT 0) encryption using 3TDEA keys */

        /** Step 1 - PIN block formation - ISO 9564-1 Format 0 */
        String pinBlock = generatePinBlock(pin, cardNumber);

        /**
         * Step 2: Generate triple length TDEA (Triple DES Encryption Algorithm) key (Session key generation same as payload encryption key except key
         * length difference)
         */
        String desKey = generateDesKey(KEY_SIZE);
        log.debug("desKey={}", desKey);

        /**
         * Step 3: Encrypt PIN block under triple length TDEA key (Same as payload encryption process)
         */
        String encryptedPin = encryptPinBlock(desKey, pinBlock);
        log.debug("encryptedPin={}", encryptedPin);

        /**
         * Step 4: Encode triple length 3DES encryption key (created in step 1) in ASN.1 DER format (Same as payload encryption process)
         */
        /** pass key length as 168 (triple length) and the Hex encoded DES key */
        String derEncodedDesKey = derEncodeWithFixIV(KEY_SIZE, desKey);

        /**
         * Step 5: Encrypt ASN.1 DER encoded DES key under the recipient’s RSA public key and hex encode it (Same as payload encryption process)
         */
        /** Step 5.1: Load RSA public key from file - Loaded during instance creation */

        /** Step 5.2 - Encrypt DER encoded DES key (created in Step 4) with RSA public key */
        /**
         * Hex.decodeHex - Converts an array of characters representing hexadecimal values into an array of bytes of those same values.
         */
        byte[] encryptedKeyBytes;
        try {
            encryptedKeyBytes = encryptKey(rsaPublicKey, Hex.decodeHex(null != derEncodedDesKey ? derEncodedDesKey.toCharArray() : null));
        } catch (Exception e) {
            log.error("Exception in encryptPin while encrypting Des key using RSA Public key", e);
            throw new ReferenceAppGenericException("Exception while encrypting pin key", e);
        }

        /** Step 5.3 - Encode encrypted double length DES key (created in Step 5.2) in hex format */
        String encryptedKey = Hex.encodeHexString(encryptedKeyBytes);

        /** Step 6: Return final output - Share the encrypted and encrypted PIN data and PIN key */
        encryptedPinBlock.encryptedKey(encryptedKey);
        encryptedPinBlock.encryptedBlock(encryptedPin);

        return encryptedPinBlock;
    }

    /**
     * From hex.
     *
     * @param c the c
     * @return the int
     */
    private int fromHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException();
    }

    /**
     * To hex.
     *
     * @param nybble the nybble
     * @return the char
     */
    private char toHex(int nybble) {
        if (nybble < 0 || nybble > 15) {
            throw new IllegalArgumentException();
        }
        return "0123456789ABCDEF".charAt(nybble);
    }

    /**
     * Xor hex.
     *
     * @param a the a
     * @param b the b
     * @return the string
     */
    private String xorHex(String a, String b) {
        char[] chars = new char[a.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = toHex(fromHex(a.charAt(i)) ^ fromHex(b.charAt(i)));
        }
        return new String(chars).toUpperCase();
    }

    /**
     * Generate pin block.
     *
     * @param pin        the pin
     * @param cardNumber the card number
     * @return the string
     * @throws ReferenceAppGenericException the exception
     */
    public String generatePinBlock(char[] pin, String cardNumber) throws ReferenceAppGenericException {
        if (pin.length < 4 || pin.length > 6) {
            log.error("In generatePinBlock invalid pin length.");
            throw new ReferenceAppGenericException("Invalid pin length.");
        }
        /** Prefix pin with zero and suffix with F to make it 16 characters long */
        String pinBlock = StringUtils.rightPad("0" + pin.length + String.valueOf(pin), 16, 'F');
        int cardLen = cardNumber.length();
        String pan = "0000" + cardNumber.substring(cardLen - 13, cardLen - 1);

        return xorHex(pinBlock, pan);
    }

    /**
     * Generate des key.
     *
     * @param length the length
     * @return the string
     */
    public String generateDesKey(int length) {
        String key = null;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ApiClientHelper.getProperty(PIN_ENCRYPTION_ALGO));
            keyGenerator.init(length);

            log.debug("Generate a secret key and extract it in encoded byte array format.");
            byte[] keyBytes = keyGenerator.generateKey()
                                          .getEncoded();

            log.debug("Convert the encrypted 'PIN Block' bytes to HEX string");
            key = Hex.encodeHexString(keyBytes);

            log.debug("Convert final key string to UPPER case string");
            key = key.toUpperCase();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return key;
    }

    /**
     * Der encode with fix IV.
     *
     * @param keyLength the key length
     * @param desKey    the des key
     * @return the string
     */
    public String derEncodeWithFixIV(int keyLength, String desKey) {
        String derEncodedDesKey = null;
        if (keyLength == 112) {
            derEncodedDesKey = "30240410" + desKey.substring(0, 32) + "041099999999999999999999999999999999";
        } else if (keyLength == 168)
            derEncodedDesKey = "302C0418" + desKey + "041099999999999999999999999999999999";

        return derEncodedDesKey;
    }

    /**
     * Gets the RSA public key.
     *
     * @param file the file
     * @return the RSA public key
     * @throws GeneralSecurityException the general security exception
     */
    public PublicKey getRSAPublicKey(String file) throws GeneralSecurityException {
        String publicKeyContent = ApiClientHelper.resourceContent(file);

        // remove public key header and footer from string
        publicKeyContent = publicKeyContent.replace("-----BEGIN PUBLIC KEY-----", "")
                                           .replace("-----END PUBLIC KEY-----", "")
                                           .replace("\r\n", "")
                                           .replace("\n", "");
        log.debug("publicKeyContent={}", publicKeyContent);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyContent));

        return kf.generatePublic(x509EncodedKeySpec);
    }

    /**
     * Encrypt key.
     *
     * @param publicKey the public key
     * @param data      the data
     * @return the byte[]
     * @throws NoSuchAlgorithmException  the no such algorithm exception
     * @throws NoSuchPaddingException    the no such padding exception
     * @throws InvalidKeyException       the invalid key exception
     * @throws IllegalBlockSizeException the illegal block size exception
     * @throws BadPaddingException       the bad padding exception
     */
    public byte[] encryptKey(PublicKey publicKey, byte[] data)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
}
