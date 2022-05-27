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

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.mastercard.developer.issuing.client.helper.RequestContext;

import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;


@Log4j2
public class OkHttpLoggingInterceptor implements Interceptor {

    /**
     * Edge device or entry application generated UUID that can be passed to downstream systems and applications to track the activity end-to-end.
     */
    public static final String CORRELATION_ID = "X-MC-Correlation-ID";

    /** The Constant CONTENT_ENCODING. */
    public static final String CONTENT_ENCODING = "content-encoding";

    /**
     * Instantiates a new ok http logging interceptor.
     *
     * @param name the name
     */
    public OkHttpLoggingInterceptor(String name) {
        log.debug("OkHttpLoggingInterceptor ({}) instance created.", name);
    }

    /**
     * Intercept.
     *
     * @param chain the chain
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();

        String correlationId = request.header(CORRELATION_ID);
        ThreadContext.put(CORRELATION_ID, correlationId);

        String reqLogged = (String) RequestContext.get("reqLogged");
        String reqMethod = request.method();

        String format = "Clear";
        if ("true".equalsIgnoreCase(reqLogged)) {
            log.info("OkHttp --> Sending request  [{}] {} with headers - \n{}", reqMethod, request.url(), request.headers());
            format = "Encrypted";
        }

        /** Cover POST, PUT, PATCH methods having request body */
        if (reqMethod.startsWith("P")) {
            Buffer requestBuffer = new Buffer();
            request.body()
                   .writeTo(requestBuffer);
            log.debug("OkHttp -- {} Request Body: {}\n", format, requestBuffer.readUtf8());
        }
        RequestContext.put("reqLogged", "true");

        Response response = chain.proceed(request);

        String correlationIdReceived = response.header(CORRELATION_ID);
        ThreadContext.put(CORRELATION_ID, correlationIdReceived);

        int responseCode = response.code();
        String respLogged = (String) RequestContext.get("respLogged");

        long t2 = System.nanoTime();

        long timeElapsed = Math.round((t2 - t1) / 1e6d);

        String content = null;
        ResponseBody body = response.body();
        if (body != null) {
            MediaType contentType = body.contentType();
            String contentEncoding = response.header(CONTENT_ENCODING);
            if ("gzip".equals(contentEncoding)) {
                BufferedSource buffer = Okio.buffer(new GzipSource(body.source()));
                content = buffer.readUtf8();
                ResponseBody wrappedBody = ResponseBody.create(content, contentType);
                response = response.newBuilder()
                                   .removeHeader(CONTENT_ENCODING)
                                   .body(wrappedBody)
                                   .build();
            } else {
                content = body.string();
                ResponseBody wrappedBody = ResponseBody.create(content, contentType);
                response = response.newBuilder()
                                   .body(wrappedBody)
                                   .build();
            }
        }

        boolean success = responseCode >= 200 && responseCode < 300;

        if (!"true".equalsIgnoreCase(respLogged)) {
            log.info("OkHttp <-- Received [{}] response for {} in {} ms with headers - \n{}", responseCode, request.url(), timeElapsed,
                    response.headers());
            // First time logging
            if (success) {
                log.debug("OkHttp -- [Success] Encrypted Response Body: {}\n", content);
                RequestContext.put("respLogged", "true");
            } else {
                log.debug("OkHttp -- [Error] Response Body: {}\n", content);
            }
        } else if (success) {
            log.debug("OkHttp -- [Success] Clear Response Body: {}\n", content);
            RequestContext.clear();
        }
        return response;
    }
}
