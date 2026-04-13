package com.namirial.sign.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namirial.sign.library.pojo.ServerErrorResponse;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
public class SignServiceClient {

    private static final String API_KEY_PROP = "namirial.server.apikey";
    private static final String API_ENDPOINT_PROP = "namirial.server.address";
    private static final String USERNAME_PROP = "namirial.server.username";
    private static final String PASSWORD_PROP = "namirial.server.password";
    private static final String MAX_CONNECTIONS_PROP = "namirial.server.max-connections";
    private static final String PENDING_ACQUIRE_TIMEOUT = "namirial.server.pending-acquire-timeout";

    public static final int DEFAULT_MAX_CONNECTIONS = 40;
    private static final int DEFAULT_PENDING_ACQUIRE_TIMEOUT = 600;

    private static final String API_KEY_HEADER_NAME = "X-SIGNBOX-EASYSIGN";
    private static final String REQUEST_ID_HEADER_NAME = "X-SIGNBOX-TRANSACTION-ID";
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    public static final String BASIC_AUTH = "Basic ";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(getMaxConnections())
            .pendingAcquireTimeout(Duration.ofSeconds(getPendingAcquireTimeout()))
            .pendingAcquireMaxCount(-1)
            .build();

    private static final HttpClient httpClient = HttpClient.create(provider);

    public static Mono<PnSignDocumentResponse> sign(String apiEndpoint, String requestId, byte[] data, String format, String level) {
        return Mono.using(

                () -> {
                    Path tempFile = Files.createTempFile("namirial-sign-", ".tmp");
                    Files.write(tempFile, data);
                    log.debug("Created temp file: {}", tempFile);
                    return tempFile;
                },

                tempFile -> httpClient
                        .headers(h -> {
                            h.set(AUTHORIZATION_HEADER_NAME, BASIC_AUTH + Base64.encodeBase64String((getUsername() + ":" + getPassword()).getBytes()));
                            h.set(API_KEY_HEADER_NAME, getApiKey());
                            h.set(REQUEST_ID_HEADER_NAME, requestId);
                        })
                        .post()
                        .uri(StringUtils.isNotBlank(apiEndpoint) ? apiEndpoint : getApiEndpoint())
                        .sendForm((req, form) -> {
                            form.multipart(true)
                                    .file("file", requestId, tempFile.toFile(), "application/octet-stream")
                                    .attr("level", level)
                                    .attr("format", format);
                        })
                        .responseSingle((response, responseBody) -> {
                            var responseId = response.responseHeaders().get(REQUEST_ID_HEADER_NAME);
                            return switch (response.status().code()) {
                                case 200 ->
                                        responseBody.asByteArray().flatMap(buffer -> parseResponse(response, buffer, responseId));
                                case 401 ->
                                        responseBody.asByteArray().flatMap(buffer -> getPermanentError(response, buffer, responseId))
                                                .switchIfEmpty(Mono.error(new PnSpapiPermanentErrorException(response.status().reasonPhrase())));
                                default ->
                                        responseBody.asByteArray().flatMap(buffer -> getTemporaryError(response, buffer, responseId))
                                                .switchIfEmpty(Mono.error(new PnSpapiTemporaryErrorException(response.status().reasonPhrase())));
                            };
                        })
                        .onErrorResume(SignServiceClient::resumeError),

                tempFile -> {
                    try {
                        Files.deleteIfExists(tempFile);
                        log.debug("Deleted temp file: {}", tempFile);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file {}: {}", tempFile, e.getMessage());
                    }
                }
        );
    }

    private static Mono<PnSignDocumentResponse> parseResponse(HttpClientResponse response, byte[] buffer, String responseId) {
        log.info("Received response from requestId {} with status code: {}, reason: {}", responseId, response.status().code(), response.status().reasonPhrase());
        return Mono.just(new PnSignDocumentResponse(buffer));
    }

    private static Mono<PnSignDocumentResponse> resumeError(Throwable t) {
        log.error("Resume from error with instanceof [{}]: {} ", ExceptionUtils.getRootCause(t).getClass().getCanonicalName(), t.getMessage());
        if (t instanceof PnSpapiPermanentErrorException) {
            return Mono.error(t);
        }
        return Mono.error(new PnSpapiTemporaryErrorException(t.getMessage(), t));
    }

    private static Mono<PnSignDocumentResponse> getTemporaryError(HttpClientResponse response, byte[] buffer, String responseId) {
        log.error("Received temporary error status code {} from requestId {} with reason: {}", response.status().code(), responseId, response.status().reasonPhrase());
        ServerErrorResponse errorResponse = getServerErrorResponse(buffer);
        if (errorResponse != null) {
            return Mono.error(new PnSpapiTemporaryErrorException(errorResponse.getDetail()));
        }
        return Mono.error(new PnSpapiTemporaryErrorException(response.status().reasonPhrase()));
    }

    private static Mono<PnSignDocumentResponse> getPermanentError(HttpClientResponse response, byte[] buffer, String responseId) {
        log.error("Received permanent status code {} from requestId {} with reason: {}", response.status().code(), responseId, response.status().reasonPhrase());
        ServerErrorResponse errorResponse = getServerErrorResponse(buffer);
        if (errorResponse != null) {
            return Mono.error(new PnSpapiPermanentErrorException(errorResponse.getDetail()));
        }
        return Mono.error(new PnSpapiPermanentErrorException(response.status().reasonPhrase()));
    }

    private static ServerErrorResponse getServerErrorResponse(byte[] buffer) {
        ServerErrorResponse errorResponse = null;
        try {
            String stringBody = new String(buffer).replace("\\", "");
            if (stringBody.startsWith("\"") && stringBody.endsWith("\"")) {
                stringBody = stringBody.substring(1, stringBody.length() - 1);
            }
            errorResponse = objectMapper.readValue(stringBody, ServerErrorResponse.class);
            log.debug("Response deserialized: {}", errorResponse);
        } catch (Exception e) {
            log.error("Error while parsing response body: {}", e.getMessage());
        }
        return errorResponse;
    }

    public static String getApiKey() {
        return StringUtils.isBlank(System.getProperty(API_KEY_PROP)) ? "" : System.getProperty(API_KEY_PROP);
    }

    public static String getApiEndpoint() {
        return StringUtils.isBlank(System.getProperty(API_ENDPOINT_PROP)) ? "" : System.getProperty(API_ENDPOINT_PROP);
    }

    public static Integer getMaxConnections() {
        return StringUtils.isBlank(System.getProperty(MAX_CONNECTIONS_PROP)) ? DEFAULT_MAX_CONNECTIONS : Integer.parseInt(System.getProperty(MAX_CONNECTIONS_PROP));
    }

    public static Integer getPendingAcquireTimeout() {
        return StringUtils.isBlank(System.getProperty(PENDING_ACQUIRE_TIMEOUT)) ? DEFAULT_PENDING_ACQUIRE_TIMEOUT : Integer.parseInt(System.getProperty(PENDING_ACQUIRE_TIMEOUT));
    }

    public static String getUsername(){
        return StringUtils.isBlank(System.getProperty(USERNAME_PROP)) ? "" : System.getProperty(USERNAME_PROP);
    }

    public static String getPassword(){
        return StringUtils.isBlank(System.getProperty(PASSWORD_PROP)) ? "" : System.getProperty(PASSWORD_PROP);
    }
}