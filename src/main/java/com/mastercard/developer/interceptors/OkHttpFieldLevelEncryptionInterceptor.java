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
package com.mastercard.developer.interceptors;

import com.mastercard.developer.encryption.EncryptionConfig;
import com.mastercard.developer.encryption.EncryptionException;
import com.mastercard.developer.encryption.FieldLevelEncryption;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.encryption.FieldLevelEncryptionParams;
import com.mastercard.developer.issuing.client.helper.RequestContext;

import lombok.extern.log4j.Log4j2;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An OkHttp3 interceptor for encrypting/decrypting parts of HTTP payloads. See: https://github.com/square/okhttp/wiki/Interceptors
 */

@Log4j2
public class OkHttpFieldLevelEncryptionInterceptor extends OkHttpEncryptionInterceptor {

    /** The Constant ENCRYPTION_PARAMS. */
    public static final String ENCRYPTION_PARAMS = "ENCRYPTION_PARAMS";

    /** The config. */
    private final FieldLevelEncryptionConfig config;

    /**
     * Instantiates a new ok http field level encryption interceptor.
     *
     * @param config the config
     */
    public OkHttpFieldLevelEncryptionInterceptor(EncryptionConfig config) {
        this.config = (FieldLevelEncryptionConfig) config;
    }

    /**
     * Encrypt payload.
     *
     * @param request        the request
     * @param requestBuilder the request builder
     * @param requestPayload the request payload
     * @return the string
     * @throws EncryptionException the encryption exception
     */
    @Override
    protected String encryptPayload(Request request, Request.Builder requestBuilder, String requestPayload) throws EncryptionException {
        if (config != null && config.useHttpHeaders()) {
            // Generate encryption params and add them as HTTP headers
            FieldLevelEncryptionParams params = FieldLevelEncryptionParams.generate(config);
            updateHeader(requestBuilder, config.getIvHeaderName(), params.getIvValue());
            updateHeader(requestBuilder, config.getEncryptedKeyHeaderName(), params.getEncryptedKeyValue());
            updateHeader(requestBuilder, config.getEncryptionCertificateFingerprintHeaderName(), config.getEncryptionCertificateFingerprint());
            updateHeader(requestBuilder, config.getEncryptionKeyFingerprintHeaderName(), config.getEncryptionKeyFingerprint());
            updateHeader(requestBuilder, config.getOaepPaddingDigestAlgorithmHeaderName(), params.getOaepPaddingDigestAlgorithmValue());
            return FieldLevelEncryption.encryptPayload(requestPayload, config, params);
        } else {
            // Encryption params will be stored in the payload
            FieldLevelEncryptionParams params = FieldLevelEncryptionParams.generate(config);
            RequestContext.put(ENCRYPTION_PARAMS, params);
            return FieldLevelEncryption.encryptPayload(requestPayload, config, params);
        }
    }

    /**
     * Decrypt payload.
     *
     * @param response        the response
     * @param responseBuilder the response builder
     * @param responsePayload the response payload
     * @return the string
     * @throws EncryptionException the encryption exception
     */
    @Override
    protected String decryptPayload(Response response, Response.Builder responseBuilder, String responsePayload) throws EncryptionException {
        if (config.useHttpHeaders()) {
            // Read encryption params from HTTP headers and delete headers
            String ivValue = response.header(config.getIvHeaderName(), null);
            String oaepPaddingDigestAlgorithmValue = response.header(config.getOaepPaddingDigestAlgorithmHeaderName(), null);
            String encryptedKeyValue = response.header(config.getEncryptedKeyHeaderName(), null);
            removeHeader(responseBuilder, config.getIvHeaderName());
            removeHeader(responseBuilder, config.getEncryptedKeyHeaderName());
            removeHeader(responseBuilder, config.getOaepPaddingDigestAlgorithmHeaderName());
            removeHeader(responseBuilder, config.getEncryptionCertificateFingerprintHeaderName());
            removeHeader(responseBuilder, config.getEncryptionKeyFingerprintHeaderName());
            FieldLevelEncryptionParams params = new FieldLevelEncryptionParams(ivValue, encryptedKeyValue, oaepPaddingDigestAlgorithmValue, config);
            return FieldLevelEncryption.decryptPayload(responsePayload, config, params);
        } else {
            // Encryption params are stored in the payload
            FieldLevelEncryptionParams params = (FieldLevelEncryptionParams) RequestContext.get(ENCRYPTION_PARAMS);
            log.debug("DecryptionKey is null - {}", config.getDecryptionKey() == null);
            return FieldLevelEncryption.decryptPayload(responsePayload, config, params);
        }
    }
}
