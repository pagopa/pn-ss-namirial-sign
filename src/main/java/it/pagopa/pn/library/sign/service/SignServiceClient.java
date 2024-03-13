package it.pagopa.pn.library.sign.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.pojo.ServerErrorResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;

import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
public class SignServiceClient {

    private static final String API_KEY_PROP = "namirial.server.apikey";
    private static final String API_ENDPOINT_PROP = "PnEcNamirialServerAddress";
    private static final String API_KEY_HEADER_NAME = "X-SIGNBOX-EASYSIGN";
    private static final String REQUEST_ID_HEADER_NAME = "X-SIGNBOX-TRANSACTION-ID";
    private static final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    private static final HttpClient httpClient = HttpClient.create();
    public static Mono<PnSignDocumentResponse> sign(String requestId, byte[] data, String format, String level) {
        return httpClient
                .headers(h -> {
                    h.set(API_KEY_HEADER_NAME, getApiKey());
                    h.set(REQUEST_ID_HEADER_NAME, requestId);
                })
                .post()
                .uri(getApiEndpoint())
                .sendForm((req, form) -> {
                    form.multipart(true)
                            .file("file", requestId, new ByteArrayInputStream(data), "application/octet-stream")
                            .attr("level", level)
                            .attr("format", format);
                })
                .responseSingle((response, responseBody) -> {
                    var responseId = response.responseHeaders().get(REQUEST_ID_HEADER_NAME);
                    return switch (response.status().code()) {
                        case 200 -> responseBody.asByteArray().flatMap(buffer -> parseResponse(response, buffer, responseId));
                        case 503 -> responseBody.asByteArray().flatMap(buffer -> getTemporaryError(response, buffer, responseId));
                        default -> responseBody.asByteArray().flatMap(buffer -> getPermanentError(response, buffer, responseId));

                    };
                })
                .onErrorResume(SignServiceClient::resumeError);
    }

    private static Mono<PnSignDocumentResponse> parseResponse(HttpClientResponse response, byte[] buffer, String responseId) {
        log.info("Received response from requestId {} with status code: {}, reason: {}", responseId, response.status().code(), response.status().reasonPhrase());
        return Mono.just(new PnSignDocumentResponse(buffer));
    }

    private static Mono<PnSignDocumentResponse> resumeError(Throwable t) {

        if (t instanceof PnSpapiTemporaryErrorException) {
            return Mono.error(t);
        }
        if(t instanceof SocketTimeoutException || t instanceof TimeoutException){
            return Mono.error(new PnSpapiTemporaryErrorException(t.getMessage(), t));
        }
        else {
            // wrap any other exception in a PnSpapiPermanentErrorException
            return Mono.error(new PnSpapiPermanentErrorException(t.getMessage(), t));
        }
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
            log.error("Response serialized: {}", stringBody);
            errorResponse = objectMapper.readValue(stringBody, ServerErrorResponse.class);
            log.debug("Response deserialized: {}", errorResponse);
        } catch (Exception e) {
            log.error("Error while parsing response body: {}", e.getMessage());
        }
        return errorResponse;
    }

    public static String getApiKey() {
        return (System.getProperty(API_KEY_PROP) == null || System.getProperty(API_KEY_PROP).trim().isEmpty())
                ? ""
                : System.getProperty(API_KEY_PROP);
    }

    public static String getApiEndpoint() {
        return (System.getenv(API_ENDPOINT_PROP) == null || System.getenv(API_ENDPOINT_PROP).trim().isEmpty())
                ? ""
                : System.getenv(API_ENDPOINT_PROP);
    }
}
