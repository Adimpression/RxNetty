/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.client;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.CookiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A Http response object used by {@link HttpClient}
 *
 * @param <T> The type of the default response content.
 *
 * @author Nitesh Kant
 */
public class HttpClientResponse<T> {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientResponse.class);

    public static final String KEEP_ALIVE_HEADER_NAME = "Keep-Alive";

    private static final Pattern PATTERN_COMMA = Pattern.compile(",");
    private static final Pattern PATTERN_EQUALS = Pattern.compile("=");
    public static final String KEEP_ALIVE_TIMEOUT_HEADER_ATTR = "timeout";

    private final HttpResponse nettyResponse;
    private final PublishSubject<T> contentSubject;
    private final HttpResponseHeaders responseHeaders;
    private final HttpVersion httpVersion;
    private final HttpResponseStatus status;
    private final CookiesHolder cookiesHolder;

    public HttpClientResponse(HttpResponse nettyResponse, PublishSubject<T> contentSubject) {
        this.nettyResponse = nettyResponse;
        this.contentSubject = contentSubject;
        httpVersion = this.nettyResponse.getProtocolVersion();
        status = this.nettyResponse.getStatus();
        responseHeaders = new HttpResponseHeaders(nettyResponse);
        cookiesHolder = CookiesHolder.newClientResponseHolder(nettyResponse.headers());
    }

    public HttpResponseHeaders getHeaders() {
        return responseHeaders;
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public Map<String, Set<Cookie>> getCookies() {
        return cookiesHolder.getAllCookies();
    }

    public Observable<T> getContent() {
        return contentSubject;
    }

    /**
     * Parses the timeout value from the HTTP keep alive header (with name {@link #KEEP_ALIVE_HEADER_NAME}) as described in
     * <a href="http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html">this spec</a>
     *
     * @return The keep alive timeout or {@code null} if this response does not define the appropriate header value.
     */
    public Long getKeepAliveTimeoutSeconds() {
        String keepAliveHeader = responseHeaders.get(KEEP_ALIVE_HEADER_NAME);
        if (null != keepAliveHeader && !keepAliveHeader.isEmpty()) {
            String[] pairs = PATTERN_COMMA.split(keepAliveHeader);
            if (pairs != null) {
                for (String pair: pairs) {
                    String[] nameValue = PATTERN_EQUALS.split(pair.trim());
                    if (nameValue != null && nameValue.length >= 2) {
                        String name = nameValue[0].trim().toLowerCase();
                        String value = nameValue[1].trim();
                        if (KEEP_ALIVE_TIMEOUT_HEADER_ATTR.equals(name)) {
                            try {
                                return Long.valueOf(value);
                            } catch (NumberFormatException e) {
                                logger.info("Invalid HTTP keep alive timeout value. Keep alive header: "
                                            + keepAliveHeader + ", timeout attribute value: " + nameValue[1], e);
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
