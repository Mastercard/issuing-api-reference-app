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
package com.mastercard.developer.encryption;

import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.ENCRYPTED_KEY;
import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.ENCRYPTED_VALUE;
import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.IV;
import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.JSON_PATH;
import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.OAEP_HASHING_ALGORITHM;
import static com.mastercard.developer.issuing.client.helper.EncryptionConfigUtil.PUBLIC_KEY_FINGERPRINT;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import com.mastercard.developer.encryption.FieldLevelEncryptionConfig.FieldValueEncoding;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class IssuingFieldLevelEncryptionConfigBuilder {

    /** The Constant NONE. */
    public static final String NONE = "NONE";

    /**
     * private constructor
     */
    private IssuingFieldLevelEncryptionConfigBuilder() {
    }

    /**
     * Gets the encryption config with none oaep padding.
     *
     * @param encryptionCertificate            the encryption certificate
     * @param encryptionCertificateFingerprint the encryption certificate fingerprint
     * @param decryptionKey the decryption key
     * @return the encryption config with none oaep padding
     */
    public static FieldLevelEncryptionConfig getEncryptionConfigWithNoneOaepPadding(Certificate encryptionCertificate,
            String encryptionCertificateFingerprint, PrivateKey decryptionKey) {
        FieldLevelEncryptionConfig config = null;
        try {
            config = new FieldLevelEncryptionConfig();
            config.encryptionCertificate = encryptionCertificate;

            Map<String, String> encryptionPaths = new HashMap<>();
            encryptionPaths.put(JSON_PATH, JSON_PATH);
            config.encryptionPaths = encryptionPaths;
            config.decryptionPaths = encryptionPaths;

            config.oaepPaddingDigestAlgorithm = NONE;
            config.oaepPaddingDigestAlgorithmFieldName = OAEP_HASHING_ALGORITHM;

            config.encryptionCertificateFingerprint = encryptionCertificateFingerprint;
            config.encryptionCertificateFingerprintFieldName = PUBLIC_KEY_FINGERPRINT;

            config.encryptedValueFieldName = ENCRYPTED_VALUE;
            config.encryptedKeyFieldName = ENCRYPTED_KEY;
            config.ivFieldName = IV;
            config.fieldValueEncoding = FieldValueEncoding.HEX;
            
            config.decryptionKey = decryptionKey;

        } catch (Exception e) {
            log.error("Error while getting encryption configuration" + e.getMessage(), e);
        }
        return config;
    }
}
