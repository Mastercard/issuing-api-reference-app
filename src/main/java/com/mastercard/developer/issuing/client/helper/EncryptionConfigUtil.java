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
package com.mastercard.developer.issuing.client.helper;

import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig.FieldValueEncoding;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfigBuilder;
import com.mastercard.developer.encryption.IssuingFieldLevelEncryptionConfigBuilder;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class EncryptionConfigUtil {

  /** Instantiates a new encryption config util. */
  private EncryptionConfigUtil() {}

  /** The Constant JSON_PATH. */
  public static final String JSON_PATH = "$";

  /** The Constant SHA_256. */
  public static final String SHA_256 = "SHA-256";

  /** The Constant SHA_512. */
  public static final String SHA_512 = "SHA-512";

  /** The Constant OAEP_HASHING_ALGORITHM. */
  public static final String OAEP_HASHING_ALGORITHM = "oaepPaddingDigestAlgorithm";

  /** The Constant ENCRYPTED_VALUE. */
  public static final String ENCRYPTED_VALUE = "encryptedValue";

  /** The Constant ENCRYPTED_KEY. */
  public static final String ENCRYPTED_KEY = "encryptedKey";

  /** The Constant PUBLIC_KEY_FINGERPRINT. */
  public static final String PUBLIC_KEY_FINGERPRINT = "publicKeyFingerprint";

  /** The Constant IV. */
  public static final String IV = "iv";

  /**
   * Gets the encryption config.
   *
   * @param encryptionCertificatePath the encryption certificate path
   * @param encryptionCertificateFingerprint the encryption certificate fingerprint
   * @param oaepPaddingDigestAlgorithm the oaep padding digest algorithm
   * @return the encryption config
   */
  public static FieldLevelEncryptionConfig getEncryptionConfig(
      String encryptionCertificatePath,
      String encryptionCertificateFingerprint,
      String oaepPaddingDigestAlgorithm) {
    FieldLevelEncryptionConfig config = null;
    try {
      log.info(
          "Load encryption certificate (RSA public key certificate issued by Mastercard KMS team): {}",
          encryptionCertificatePath);
      Certificate encryptionCertificate =
          loadEncryptionCertificate(ApiClientHelper.getFileInputStream(encryptionCertificatePath));

      if (oaepPaddingDigestAlgorithm != null
          && (oaepPaddingDigestAlgorithm.equalsIgnoreCase(SHA_256)
              || oaepPaddingDigestAlgorithm.equalsIgnoreCase(SHA_512))) {
        config =
            buildEncryptionConfig(
                encryptionCertificate,
                encryptionCertificateFingerprint,
                oaepPaddingDigestAlgorithm);
      } else {
        log.warn("Using OaepPaddingDigestAlgorithm=NONE");
        config =
            IssuingFieldLevelEncryptionConfigBuilder.getEncryptionConfigWithNoneOaepPadding(
                encryptionCertificate, encryptionCertificateFingerprint);
      }

    } catch (Exception e) {
      log.error("Error while getting encryption configuration" + e.getMessage(), e);
    }
    return config;
  }

  /**
   * Build a {@link com.mastercard.developer.encryption.FieldLevelEncryptionConfig}.
   *
   * @param encryptionCertificate the encryption certificate
   * @param encryptionCertificateFingerprint the encryption certificate fingerprint
   * @param oaepPaddingDigestAlgorithm the oaep padding digest algorithm
   * @return the field level encryption config
   */
  public static FieldLevelEncryptionConfig buildEncryptionConfig(
      Certificate encryptionCertificate,
      String encryptionCertificateFingerprint,
      String oaepPaddingDigestAlgorithm) {
    FieldLevelEncryptionConfig config = null;
    try {
      // Use this block for using SHA-256 & SHA-512 OAEP padding
      config =
          FieldLevelEncryptionConfigBuilder.aFieldLevelEncryptionConfig()
              .withEncryptionCertificate(encryptionCertificate)
              .withEncryptionPath(JSON_PATH, JSON_PATH)
              .withDecryptionKey(null)
              .withDecryptionPath(JSON_PATH, JSON_PATH)
              .withEncryptedValueFieldName(ENCRYPTED_VALUE)
              .withEncryptedKeyFieldName(ENCRYPTED_KEY)
              .withEncryptionCertificateFingerprintFieldName(PUBLIC_KEY_FINGERPRINT)
              .withEncryptionCertificateFingerprint(encryptionCertificateFingerprint)
              .withOaepPaddingDigestAlgorithmFieldName(OAEP_HASHING_ALGORITHM)
              .withOaepPaddingDigestAlgorithm(oaepPaddingDigestAlgorithm)
              .withIvFieldName(IV)
              .withFieldValueEncoding(FieldValueEncoding.HEX)
              .build();
    } catch (Exception e) {
      log.error("Error while getting encryption configuration" + e.getMessage(), e);
    }
    return config;
  }

  /**
   * Populate a X509 encryption certificate object with the certificate data at the given file path.
   *
   * @param certificateInputStream the certificate input stream
   * @return the certificate
   * @throws CertificateException the certificate exception
   * @throws FileNotFoundException the file not found exception
   */
  public static Certificate loadEncryptionCertificate(InputStream certificateInputStream)
      throws CertificateException {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    return factory.generateCertificate(certificateInputStream);
  }
}
