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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.interceptors.OkHttpFieldLevelEncryptionInterceptor;
import com.mastercard.developer.interceptors.OkHttpGetRequestEncryptionHeaderInterceptor;
import com.mastercard.developer.interceptors.OkHttpLoggingInterceptor;
import com.mastercard.developer.issuing.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;

import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

/** The Constant log. */

/** The Constant log. */
@Log4j2
public final class ApiClientHelper {

    /** The Constant EXCEPTION. */
    public static final String EXCEPTION = "Exception";

    /** Instantiates a new request helper. */
    private ApiClientHelper() {
    }

    /** The Constant DEBUG_MODE. */
    private static final String DEBUG_MODE = "mi.client.debug.mode";

    /** The Constant DATE_TIME_FORMATTER. */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX");

    /** The Constant BASE_URL. */
    private static final String BASE_URL = "mi.api.base.path";

    /** The Constant MTLS_KEYSTORE_PATH. */
    private static final String MTLS_KEYSTORE_PATH = "mi.api.mtls.keystore.file";

    /** The Constant MTLS_KEYSTORE_PWORD. */
    private static final String MTLS_KEYSTORE_PWORD = "mi.api.mtls.keystore.password";

    /** The Constant ENCRYPTION_KEYSTORE_PATH. */
    private static final String ENCRYPTION_KEYSTORE_PATH = "mi.api.encryption.public.key.file";

    /** The Constant ENCRYPTION_KEY_FINGERPRINT. */
    private static final String ENCRYPTION_KEY_FINGERPRINT = "mi.api.encryption.public.key.fingerprint";

    /** The Constant ENCRYPTION_OAEP_ALGORITHM. */
    private static final String ENCRYPTION_OAEP_ALGORITHM = "mi.api.encryption.oaep.algorithm";

    /** The Constant DECRYPTION_PRIVATE_KEY_KEYSTORE_PATH. */
    private static final String DECRYPTION_PRIVATE_KEY_KEYSTORE_PATH = "mi.api.encryption.private.key.keystore.file";

    /** The Constant DECRYPTION_PRIVATE_KEY_KEYSTORE_PWORD. */
    private static final String DECRYPTION_PRIVATE_KEY_KEYSTORE_PWORD = "mi.api.encryption.private.key.keystore.password";

    /** The Constant DECRYPTION_PRIVATE_KEY_ALIAS. */
    private static final String DECRYPTION_PRIVATE_KEY_ALIAS = "mi.api.encryption.private.key.alias";

    /** The prop. */
    private static Properties prop = null;

    /** The property file. */
    private static String propertyFile = "application.properties";

    /** The object mapper. */
    private static ObjectMapper objectMapper = createObjectMapper();

    /** The builder map. */
    private static Map<String, OkHttpClient.Builder> builderMap = new HashMap<>();

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
                String env = System.getProperty("env");
                if (StringUtils.isEmpty(env)) {
                    log.info("Environment not set, use default env: sandbox");
                    env = "sandbox";
                }
                propertyFile = env + "/" + propertyFile;
                log.info("Try to load the properties file: {} ", propertyFile);
                InputStream input = ApiClientHelper.class.getClassLoader()
                                                         .getResourceAsStream(propertyFile);
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
         * It is recommended to create an instance of ApiClient per thread in a multi-threaded environment to avoid any potential issues.
         */
        ApiClient client = new ApiClient();
        OkHttpClient.Builder httpClientBuilder = null;

        boolean debugMode = false;
        try {
            debugMode = Boolean.parseBoolean(prop.getProperty(DEBUG_MODE));

            String mtlsKeystorePath = prop.getProperty(MTLS_KEYSTORE_PATH);
            httpClientBuilder = builderMap.get(mtlsKeystorePath);
            if (httpClientBuilder == null) {
                httpClientBuilder = client.getHttpClient()
                                          .newBuilder();
                httpClientBuilder = setSSLContext(httpClientBuilder, mtlsKeystorePath, prop.getProperty(MTLS_KEYSTORE_PWORD)
                                                                                           .toCharArray());
                FieldLevelEncryptionConfig encryptionConfig = EncryptionConfigUtil.getEncryptionConfig(prop.getProperty(ENCRYPTION_KEYSTORE_PATH),
                        prop.getProperty(ENCRYPTION_KEY_FINGERPRINT), prop.getProperty(ENCRYPTION_OAEP_ALGORITHM),
                        prop.getProperty(DECRYPTION_PRIVATE_KEY_KEYSTORE_PATH), prop.getProperty(DECRYPTION_PRIVATE_KEY_KEYSTORE_PWORD),
                        prop.getProperty(DECRYPTION_PRIVATE_KEY_ALIAS));

                if (debugMode) {
                    httpClientBuilder = httpClientBuilder.addInterceptor(new OkHttpLoggingInterceptor("Before"));
                    log.info("DebugMode - Added OkHttpLoggingInterceptor(Before) interceptor for logging purpose");
                }

                httpClientBuilder = httpClientBuilder.addInterceptor(new OkHttpGetRequestEncryptionHeaderInterceptor(encryptionConfig));
                httpClientBuilder = httpClientBuilder.addInterceptor(new OkHttpFieldLevelEncryptionInterceptor(encryptionConfig));
                log.info("Added OkHttpFieldLevelEncryptionInterceptor interceptor to support payload encryption/decryption");

                if (debugMode) {
                    httpClientBuilder = httpClientBuilder.addInterceptor(new OkHttpLoggingInterceptor("After"));
                    log.info("DebugMode - Added OkHttpLoggingInterceptor(After) interceptor for logging purpose");
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
            System.exit(-1);
        }

        return client;
    }

    /**
     * Gets the request object.
     *
     * @param <T>      the generic type
     * @param scenario the scenario
     * @param cls      the cls
     * @return the request object
     */
    public static <T> T getRequestObject(String scenario, Class<T> cls) {
        T returnValue = null;
        try {
            StringBuilder filePath = new StringBuilder("sample_requests/");
            filePath.append(scenario.toLowerCase());
            filePath.append("-request.json");
            String requestPayload = resourceContent(filePath.toString());

            if (cls.getName()
                   .equalsIgnoreCase("java.lang.String")) {
                returnValue = (T) requestPayload;
            } else {
                returnValue = objectMapper.readValue(requestPayload, cls);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return returnValue;
    }

    /**
     * Save response object.
     *
     * @param scenario       the scenario
     * @param responseObject the response object
     */
    public static void saveResponseObject(String scenario, Object responseObject) {
        try {
            File outputFile = new File("sample_responses/", scenario.toLowerCase() + (responseObject == null ? " - ERROR" : "") + "-response.json");
            outputFile.getParentFile()
                      .mkdirs();

            if (responseObject == null) {
                responseObject = RequestContext.get(EXCEPTION);
            }
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(outputFile, responseObject);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Sets the SSL context.
     *
     * @param phttpClientBuilder      the phttp client builder
     * @param mtlsCertificatePath     the mtls certificate path
     * @param mtlsCertificatePassword the mtls certificate password
     * @return the ok http client. builder
     * @throws ReferenceAppGenericException the reference app generic exception
     */
    private static OkHttpClient.Builder setSSLContext(OkHttpClient.Builder phttpClientBuilder, String mtlsCertificatePath,
            char[] mtlsCertificatePassword) throws ReferenceAppGenericException {
        SSLContext sslContext = null;
        OkHttpClient.Builder httpClientBuilder = null;
        try {
            log.info("Load your client certificate (Private certificate signed by Mastercard KMS team): {}", mtlsCertificatePath);
            // Load your client certificate (certificate issued by Mastercard KMS team)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(getFileInputStream(mtlsCertificatePath), mtlsCertificatePassword);

            log.info("Load pkcs12KeyStore in TrustManagerFactory");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length < 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            // Configure a secure socket
            log.info("Load pkcs12KeyStore in KeyManagerFactory");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, mtlsCertificatePassword);

            log.info("Create SSLContext");
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            log.info("set SSLContext to OkHttpClient.Builder");
            httpClientBuilder = phttpClientBuilder.sslSocketFactory(sslSocketFactory, trustManager);

        } catch (Exception e) {
            log.error("Exception occurred while initializing SSLContext", e);
            throw new ReferenceAppGenericException("Exception occurred while initializing SSLContext in ApiClientHelper", e);
        }
        return httpClientBuilder;
    }

    /**
     * Resource content.
     *
     * @param filePath the file path
     * @return the string
     */
    public static String resourceContent(String filePath) {
        String response = null;
        try {
            log.debug("Load sample request from {} file", filePath);

            InputStream inputStream = getFileInputStream(filePath);
            response = IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot load resource from classpath '" + filePath + "'.", ex);
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
        InputStream inputStream = ApiClientHelper.class.getClassLoader()
                                                       .getResourceAsStream(inputFile);

        if (inputStream == null) {
            log.debug("File {} not found. Lets try to load from classpath.", inputFile);
            inputStream = ApiClientHelper.class.getResourceAsStream("/" + inputFile);
        }

        if (inputStream == null) {
            File file = new File(inputFile);
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                log.fatal("CONFIGURATION_ERROR: File {} not found.", inputFile);
                throw new FileNotFoundException(inputFile);
            }
        }
        return inputStream;
    }

    public static URL getFileURL(String inputFile) throws FileNotFoundException, MalformedURLException {
        URL url = ApiClientHelper.class.getClassLoader()
                                       .getResource(inputFile);

        if (url == null) {
            log.debug("File {} not found. Lets try to load from classpath.", inputFile);
            url = ApiClientHelper.class.getResource("/" + inputFile);
        }

        if (url == null) {
            File file = new File(inputFile);
            if (file.exists()) {
                url = file.toURI()
                          .toURL();
            } else {
                log.error("File {} not found.", inputFile);
            }
        }
        return url;
    }

    /**
     * Creates the object mapper.
     *
     * @return the object mapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime offsetDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                    throws IOException {
                jsonGenerator.writeString(DATE_TIME_FORMATTER.format(offsetDateTime));
            }
        });
        objectMapper.registerModule(simpleModule);

        simpleModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                return OffsetDateTime.parse(parser.getText(), DATE_TIME_FORMATTER);
            }
        });
        objectMapper.registerModule(simpleModule);

        /** Ignore unknown fields */
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /** Exclude fields having null or empty values */
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);

        return objectMapper;
    }
}
