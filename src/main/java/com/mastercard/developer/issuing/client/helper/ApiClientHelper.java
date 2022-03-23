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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.exception.ReferenceAppGenericException;
import com.mastercard.developer.interceptors.OkHttpFieldLevelEncryptionInterceptor;
import com.mastercard.developer.interceptors.OkHttpGetRequestEncryptionHeaderInterceptor;
import com.mastercard.developer.interceptors.OkHttpLoggingInterceptor;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;

/** The Constant log. */
@Log4j2
public final class ApiClientHelper {

  /** The Constant DEBUG_MODE. */
  private static final String DEBUG_MODE = "mastercard.issuing.client.debug.mode";

  /** The Constant DATE_TIME_FORMATTER. */
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX");

  /** The Constant BASE_URL. */
  private static final String BASE_URL = "mastercard.issuing.client.api.base.path";

  /** The Constant MTLS_KEYSTORE_PATH. */
  private static final String MTLS_KEYSTORE_PATH =
      "mastercard.issuing.client.ref.app.mtls.keystore.file";

  /** The Constant MTLS_KEYSTORE_PWORD. */
  private static final String MTLS_KEYSTORE_PWORD =
      "mastercard.issuing.client.ref.app.mtls.keystore.password";

  /** The Constant ENCRYPTION_KEYSTORE_PATH. */
  private static final String ENCRYPTION_KEYSTORE_PATH =
      "mastercard.issuing.client.ref.app.encryption.public.key.file";

  /** The Constant ENCRYPTION_KEY_FINGERPRINT. */
  private static final String ENCRYPTION_KEY_FINGERPRINT =
      "mastercard.issuing.client.ref.app.encryption.public.key.fingerprint";

  /** The Constant ENCRYPTION_OAEP_ALGORITHM. */
  private static final String ENCRYPTION_OAEP_ALGORITHM =
      "mastercard.issuing.client.ref.app.encryption.oaep.algorithm";

  /** The prop. */
  private static Properties prop = null;

  /** The property file. */
  private static String propertyFile = "application.properties";

  /** The object mapper. */
  private static ObjectMapper objectMapper = createObjectMapper();

  /** The builder map. */
  private static Map<String, OkHttpClient.Builder> builderMap = new HashMap<>();

  /** Instantiates a new request helper. */
  private ApiClientHelper() {}

  /**
   * Sets the prop.
   *
   * @param prop the new prop
   */
  public static void setProp(Properties prop) {
    ApiClientHelper.prop = prop;
  }

  /**
   * Sets the property file.
   *
   * @param propertyFile the new property file
   */
  public static void setPropertyFile(String propertyFile) {
    ApiClientHelper.propertyFile = propertyFile;
  }

  /** Load properties. */
  public static void loadProperties() {
    if (prop == null || prop.isEmpty()) {
      try {
        InputStream input =
            ApiClientHelper.class.getClassLoader().getResourceAsStream(propertyFile);
        prop = new Properties();
        if (input == null) {
          return;
        }
        prop.load(input);
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  /**
   * Gets the property.
   *
   * @param key the key
   * @return the property
   */
  public static String getProperty(String key) {
    loadProperties();
    return prop.getProperty(key);
  }

  /**
   * Gets the api client.
   *
   * @param context the context
   * @return the api client
   */
  public static ApiClient getApiClient(String context) {
    loadProperties();

    /**
     * It is recommended to create an instance of ApiClient per thread in a multi-threaded
     * environment to avoid any potential issues.
     */
    ApiClient client = new ApiClient();
    OkHttpClient.Builder httpClientBuilder = null;

    boolean debugMode = false;
    try {
      debugMode = Boolean.parseBoolean(prop.getProperty(DEBUG_MODE));

      String mtlsKeystorePath = prop.getProperty(MTLS_KEYSTORE_PATH);
      httpClientBuilder = builderMap.get(mtlsKeystorePath);
      if (httpClientBuilder == null) {
        httpClientBuilder = client.getHttpClient().newBuilder();
        httpClientBuilder =
            setSSLContext(
                httpClientBuilder,
                mtlsKeystorePath,
                prop.getProperty(MTLS_KEYSTORE_PWORD).toCharArray());
        FieldLevelEncryptionConfig encryptionConfig =
            EncryptionConfigUtil.getEncryptionConfig(
                prop.getProperty(ENCRYPTION_KEYSTORE_PATH),
                prop.getProperty(ENCRYPTION_KEY_FINGERPRINT),
                prop.getProperty(ENCRYPTION_OAEP_ALGORITHM));

        if (debugMode) {
          httpClientBuilder =
              httpClientBuilder.addInterceptor(new OkHttpLoggingInterceptor("Before"));
          log.info(
              "DebugMode - Added OkHttpLoggingInterceptor(Before) interceptor for logging purpose");
        }

        httpClientBuilder =
            httpClientBuilder.addInterceptor(
                new OkHttpGetRequestEncryptionHeaderInterceptor(encryptionConfig));
        httpClientBuilder =
            httpClientBuilder.addInterceptor(
                new OkHttpFieldLevelEncryptionInterceptor(encryptionConfig));
        log.info(
            "Added OkHttpFieldLevelEncryptionInterceptor interceptor to support payload encryption/decryption");

        if (debugMode) {
          httpClientBuilder =
              httpClientBuilder.addInterceptor(new OkHttpLoggingInterceptor("After"));
          log.info(
              "DebugMode - Added OkHttpLoggingInterceptor(After) interceptor for logging purpose");
        }
        builderMap.put(mtlsKeystorePath, httpClientBuilder);
      }

      client.setOffsetDateTimeFormat(DATE_TIME_FORMATTER);

      // Configure the Mastercard service URL
      client.setBasePath(prop.getProperty(BASE_URL) + context);

      client.setHttpClient(httpClientBuilder.build());

      // Disabled OK Http debugging
      client.setDebugging(false);

    } catch (Exception e) {
      log.fatal("Unable to load the SSL context or setup encryption config.", e);
    }

    return client;
  }

  /**
   * Gets the request object.
   *
   * @param <T> the generic type
   * @param scenario the scenario
   * @param cls the cls
   * @return the request object
   */
  public static <T> T getRequestObject(String scenario, Class<T> cls) {
    T returnValue = null;
    try {
      StringBuilder filePath = new StringBuilder("sample_requests/");
      filePath.append(scenario.toLowerCase());
      filePath.append("-request.json");
      String searchCardsReqPayload = resourceContent(filePath.toString());

      if (cls.getName().equalsIgnoreCase("java.lang.String")) {
        returnValue = (T) searchCardsReqPayload;
      } else {
        returnValue = objectMapper.readValue(searchCardsReqPayload, cls);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return returnValue;
  }

  /**
   * Sets the SSL context.
   *
   * @param httpClientBuilder the http client builder
   * @param mtlsCertificatePath the mtls certificate path
   * @param mtlsCertificatePassword the mtls certificate password
   * @return the ok http client. builder
   * @throws Exception the exception
   */
  private static OkHttpClient.Builder setSSLContext(
      OkHttpClient.Builder phttpClientBuilder,
      String mtlsCertificatePath,
      char[] mtlsCertificatePassword)
      throws ReferenceAppGenericException {
    SSLContext sslContext = null;
    OkHttpClient.Builder httpClientBuilder = null;
    try {
      log.info(
          "Load your client certificate (Private certificate signed by Mastercard KMS team): {}",
          mtlsCertificatePath);
      // Load your client certificate (certificate issued by Mastercard KMS team)
      KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
      pkcs12KeyStore.load(getFileInputStream(mtlsCertificatePath), mtlsCertificatePassword);

      log.info("Load pkcs12KeyStore in TrustManagerFactory");
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(pkcs12KeyStore);

      // Configure a secure socket
      log.info("Load pkcs12KeyStore in KeyManagerFactory");
      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(pkcs12KeyStore, mtlsCertificatePassword);

      log.info("Create SSLContext");
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

      log.info("set SSLContext to OkHttpClient.Builder");
      httpClientBuilder =
          phttpClientBuilder.sslSocketFactory(
              sslContext.getSocketFactory(),
              (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);

    } catch (Exception e) {
      log.error("Exception occurred while initializing SSLContext", e);
      throw new ReferenceAppGenericException(
          "Exception occurred while initializing SSLContext in ApiClientHelper", e);
    }
    return httpClientBuilder;
  }

  /**
   * Resource content.
   *
   * @param filePath the file path
   * @return the string
   */
  private static String resourceContent(String filePath) {
    String response = null;
    try {
      log.debug("Load sample request from {} file", filePath);

      InputStream inputStream = getFileInputStream(filePath);
      response =
          IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8.name());
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Cannot load resource from classpath '" + filePath + "'.", ex);
    }
    return response;
  }

  /**
   * Gets the file input stream.
   *
   * @param inputFile the input file
   * @return the file input stream
   * @throws FileNotFoundException the file not found exception
   */
  public static InputStream getFileInputStream(String inputFile) throws FileNotFoundException {
    InputStream inputStream = ApiClientHelper.class.getClassLoader().getResourceAsStream(inputFile);

    if (inputStream == null) {
      log.debug("File {} not found. Lets try to load from classpath.", inputFile);
      inputStream = ApiClientHelper.class.getResourceAsStream("/" + inputFile);
    }

    if (inputStream == null) {
      File file = new File(inputFile);
      if (file.exists()) {
        inputStream = new FileInputStream(file);
      } else {
        log.error("File {} not found.", inputFile);
      }
    }
    return inputStream;
  }

  /**
   * Creates the object mapper.
   *
   * @return the object mapper
   */
  private static ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(
        OffsetDateTime.class,
        new JsonSerializer<OffsetDateTime>() {
          @Override
          public void serialize(
              OffsetDateTime offsetDateTime,
              JsonGenerator jsonGenerator,
              SerializerProvider serializerProvider)
              throws IOException {
            jsonGenerator.writeString(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(offsetDateTime));
          }
        });
    objectMapper.registerModule(simpleModule);

    simpleModule.addDeserializer(
        OffsetDateTime.class,
        new JsonDeserializer<OffsetDateTime>() {
          @Override
          public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context)
              throws IOException {
            return OffsetDateTime.parse(
                parser.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
          }
        });
    objectMapper.registerModule(simpleModule);

    return objectMapper;
  }
}
