/*
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
package com.mastercard.developer.encryption.rsa;

import com.mastercard.developer.encryption.EncryptionException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/** The Class RSA. */
public class RSA {

  /** Instantiates a new rsa. */
  private RSA() {
    // Nothing to do here
  }

  /** The Constant ASYMMETRIC_CYPHER. */
  private static final String ASYMMETRIC_CYPHER = "RSA/ECB/OAEPWith{ALG}AndMGF1Padding";

  /** The Constant DEFAULT_PKCS1PADDING. */
  public static final String DEFAULT_PKCS1PADDING = "RSA/ECB/PKCS1Padding";

  /** The Constant SYMMETRIC_KEY_TYPE. */
  private static final String SYMMETRIC_KEY_TYPE = "AES";

  /**
   * Wrap secret key.
   *
   * @param publicKey the public key
   * @param privateKey the private key
   * @param oaepDigestAlgorithm the oaep digest algorithm
   * @return the byte[]
   * @throws EncryptionException the encryption exception
   */
  public static byte[] wrapSecretKey(
      PublicKey publicKey, Key privateKey, String oaepDigestAlgorithm) throws EncryptionException {
    try {
      String asymmetricCipher = DEFAULT_PKCS1PADDING;
      MGF1ParameterSpec mgf1ParameterSpec = null;
      if (!oaepDigestAlgorithm.equalsIgnoreCase("NONE")) {
        mgf1ParameterSpec = new MGF1ParameterSpec(oaepDigestAlgorithm);
        asymmetricCipher =
            ASYMMETRIC_CYPHER.replace("{ALG}", mgf1ParameterSpec.getDigestAlgorithm());
      }
      Cipher cipher = Cipher.getInstance(asymmetricCipher);

      if (mgf1ParameterSpec != null) {
        cipher.init(Cipher.WRAP_MODE, publicKey, getOaepParameterSpec(mgf1ParameterSpec));
      } else {
        cipher.init(Cipher.WRAP_MODE, publicKey);
      }

      return cipher.wrap(privateKey);
    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Failed to wrap secret key!", e);
    }
  }

  /**
   * Unwrap secret key.
   *
   * @param decryptionKey the decryption key
   * @param keyBytes the key bytes
   * @param oaepDigestAlgorithm the oaep digest algorithm
   * @return the key
   * @throws EncryptionException the encryption exception
   */
  public static Key unwrapSecretKey(
      PrivateKey decryptionKey, byte[] keyBytes, String oaepDigestAlgorithm)
      throws EncryptionException {
    if (!oaepDigestAlgorithm.contains("-")) {
      oaepDigestAlgorithm = oaepDigestAlgorithm.replace("SHA", "SHA-");
    }
    try {
      String asymmetricCipher = DEFAULT_PKCS1PADDING;
      MGF1ParameterSpec mgf1ParameterSpec = null;
      if (!oaepDigestAlgorithm.equalsIgnoreCase("NONE")) {
        mgf1ParameterSpec = new MGF1ParameterSpec(oaepDigestAlgorithm);
        asymmetricCipher =
            ASYMMETRIC_CYPHER.replace("{ALG}", mgf1ParameterSpec.getDigestAlgorithm());
      }

      Cipher cipher = Cipher.getInstance(asymmetricCipher);

      if (mgf1ParameterSpec != null) {
        cipher.init(Cipher.UNWRAP_MODE, decryptionKey, getOaepParameterSpec(mgf1ParameterSpec));
      } else {
        cipher.init(Cipher.UNWRAP_MODE, decryptionKey);
      }

      return cipher.unwrap(keyBytes, SYMMETRIC_KEY_TYPE, Cipher.SECRET_KEY);

    } catch (GeneralSecurityException e) {
      throw new EncryptionException("Failed to unwrap secret key!", e);
    }
  }

  /**
   * Gets the oaep parameter spec.
   *
   * @param mgf1ParameterSpec the mgf 1 parameter spec
   * @return the oaep parameter spec
   */
  private static OAEPParameterSpec getOaepParameterSpec(MGF1ParameterSpec mgf1ParameterSpec) {
    return new OAEPParameterSpec(
        mgf1ParameterSpec.getDigestAlgorithm(),
        "MGF1",
        mgf1ParameterSpec,
        PSource.PSpecified.DEFAULT);
  }
}
