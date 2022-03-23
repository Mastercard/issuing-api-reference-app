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
package com.mastercard.developer.interceptors;

import com.mastercard.developer.encryption.EncryptionConfig;
import com.mastercard.developer.encryption.EncryptionException;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.encryption.FieldLevelEncryptionParams;
import com.mastercard.developer.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.client.helper.RequestContext;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An OkHttp3 interceptor for sending encryption key in HTTP header. See:
 * https://github.com/square/okhttp/wiki/Interceptors
 */

/** The Constant log. */
@Log4j2
public class OkHttpGetRequestEncryptionHeaderInterceptor implements Interceptor {

  /** The Constant OAEP_HASHING_ALGORITHM. */
  public static final String OAEP_HASHING_ALGORITHM_HEADER = "X-MC-Oaep-Padding-Digest-Algorithm";

  /** The Constant ENCRYPTED_KEY. */
  public static final String ENCRYPTED_KEY_HEADER = "X-MC-Encrypted-Key";

  /** The Constant PUBLIC_KEY_FINGERPRINT. */
  public static final String PUBLIC_KEY_FINGERPRINT_HEADER = "X-MC-Public-Key-Fingerprint";

  /** The Constant IV. */
  public static final String IV_HEADER = "X-MC-IV";

  /** The config. */
  private final FieldLevelEncryptionConfig config;

  /**
   * Instantiates a new ok http get request encryption header interceptor.
   *
   * @param config the config
   */
  public OkHttpGetRequestEncryptionHeaderInterceptor(EncryptionConfig config) {
    this.config = (FieldLevelEncryptionConfig) config;
  }

  /**
   * Intercept.
   *
   * @param chain the chain
   * @return the response
   */
  @Override
  public Response intercept(Chain chain) {
    Response response = null;
    Request request;
    try {
      request = chain.request();
      String reqMethod = request.method();
      if (reqMethod.equalsIgnoreCase("GET")) {
        request = addEncryptionHeaders(request);
      }
      response = chain.proceed(request);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return response;
  }

  /**
   * Adds the encryption headers.
   *
   * @param request the request
   * @return the request
   * @throws Exception the exception
   */
  protected Request addEncryptionHeaders(Request request) throws ReferenceAppGenericException {

    Request.Builder requestBuilder = request.newBuilder();
    // Generate encryption params and add them as HTTP headers
    FieldLevelEncryptionParams params;
    try {
      params = FieldLevelEncryptionParams.generate(config);
    } catch (EncryptionException e) {
      log.error("Exception in method addEncryptionHeaders", e);
      throw new ReferenceAppGenericException("Exception while addEncryptionHeaders", e);
    }

    updateHeader(requestBuilder, IV_HEADER, params.getIvValue());
    updateHeader(requestBuilder, ENCRYPTED_KEY_HEADER, params.getEncryptedKeyValue());
    updateHeader(
        requestBuilder,
        PUBLIC_KEY_FINGERPRINT_HEADER,
        config.getEncryptionCertificateFingerprint());
    updateHeader(
        requestBuilder, OAEP_HASHING_ALGORITHM_HEADER, params.getOaepPaddingDigestAlgorithmValue());

    RequestContext.put(OkHttpFieldLevelEncryptionInterceptor.ENCRYPTION_PARAMS, params);
    return requestBuilder.build();
  }

  /**
   * Update header.
   *
   * @param requestBuilder the request builder
   * @param name the name
   * @param value the value
   */
  static void updateHeader(Request.Builder requestBuilder, String name, String value) {
    if (name != null && value != null) {
      requestBuilder.header(name, value);
    }
  }
}
