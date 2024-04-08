package it.pagopa.pn.library.sign.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namirial.sign.library.pojo.ServerErrorResponse;
import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
public class PnSignServiceImplTest {
    private static MockWebServer mockWebServer;
    private static PnSignService signService;
    private static PnSignService defaultClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        signService = new PnSignServiceImpl(mockWebServer.url("/").toString());
        defaultClient = new PnSignServiceImpl();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Sign a PDF file using PADES format and BES and T level correctly")
    void testSignPdfDocument() throws IOException {

        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        var okResponse = getOkResponse(bytes);

        mockWebServer.enqueue(okResponse);
        mockWebServer.enqueue(okResponse);

        Mono<PnSignDocumentResponse> responseBes = signService.signPdfDocument(bytes, false);
        Mono<PnSignDocumentResponse> responseT = signService.signPdfDocument(bytes, true);

        checkAssertionsOk(responseBes, responseT);
    }

    @Test
    @DisplayName("Try to sign a PDF file and handle possible failures")
    void testSignPdfDocumentWithErrors() throws IOException {

        // Handle null input
        var responseMonoWithNull = signService.signPdfDocument(null, false);
        StepVerifier.create(responseMonoWithNull).expectErrorMatches(t -> t instanceof PnSpapiPermanentErrorException).verify();

        // Prepare resources
        var bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));

        // Mock responses
        produceErrorResponse();

        // Assertions on the responses
        var responseMono = signService.signPdfDocument(bytes, false);
        checkAssertionsBasedOnErrors(responseMono);

    }

    @Test
    @DisplayName("Sign a XML file using XADES format and BES and T level correctly")
    void testSignXmlDocument() throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.xml"));
        var okResponse = getOkResponse(bytes);

        mockWebServer.enqueue(okResponse);
        mockWebServer.enqueue(okResponse);

        var signResponseBes = signService.signXmlDocument(bytes, false);
        var signResponseT = signService.signXmlDocument(bytes, true);

        checkAssertionsOk(signResponseBes, signResponseT);
    }

    @Test
    @DisplayName("Try to sign a XML file and handle possible failures")
    void testSignXmlDocumentWithErrors() throws IOException {

        // Handle null input
        var responseMonoWithNull = signService.signXmlDocument(null, false);
        StepVerifier.create(responseMonoWithNull).expectErrorMatches(t -> t instanceof PnSpapiPermanentErrorException).verify();

        // Prepare resources
        var bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        produceErrorResponse();

        // Assertions on the responses
        var responseMono = signService.signXmlDocument(bytes, false);
        checkAssertionsBasedOnErrors(responseMono);
    }

    @Test
    @DisplayName("Sign a PDF file using CADES format and BES and T level correctly")
    void testSignPck7Document() throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        var okResponse = getOkResponse(bytes);

        mockWebServer.enqueue(okResponse);
        mockWebServer.enqueue(okResponse);

        var signResponseBes = signService.pkcs7Signature(bytes, false);
        var signResponseT = signService.pkcs7Signature(bytes, true);

        checkAssertionsOk(signResponseBes, signResponseT);
    }
    @Test
    @DisplayName("Try to sign a PDF file using PKCS7 format and handle possible failures")
    void testSignPkcs7DocumentWithErrors() throws IOException {

        // Handle null input
        var responseMonoWithNull = signService.pkcs7Signature(null, false);
        StepVerifier.create(responseMonoWithNull).expectErrorMatches(t -> t instanceof PnSpapiPermanentErrorException).verify();

        // Prepare resources
        var bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));

        // Mock responses
        produceErrorResponse();

        // Assertions on the responses
        var responseMono = signService.pkcs7Signature(bytes, false);
        checkAssertionsBasedOnErrors(responseMono);
    }

    @Test
    @DisplayName("Test IOException handling")
    void testInternalException() throws IOException {
        mockWebServer.shutdown();
        var pdf = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        var xml = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.xml"));

        var responseMonoPades = signService.signPdfDocument(pdf, false);
        var responseMonoXades = signService.signXmlDocument(xml, false);
        var responseMonoPkcs7 = signService.pkcs7Signature(pdf, false);
        StepVerifier.create(responseMonoPades).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMonoXades).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMonoPkcs7).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
    }

    @Test
    @DisplayName("Test initialization with default client")
    void testDefaultClientInit() throws IOException {
        var pdf = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        var xml = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.xml"));

        // Fails for empty environment variable
        var responseMonoPades = defaultClient.signPdfDocument(pdf, false);
        var responseMonoXades = defaultClient.signXmlDocument(xml, false);
        var responseMonoPkcs7 = defaultClient.pkcs7Signature(pdf, false);
        StepVerifier.create(responseMonoPades).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMonoXades).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMonoPkcs7).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
    }


    private static void checkAssertionsOk(Mono<PnSignDocumentResponse> signResponseBes, Mono<PnSignDocumentResponse> signResponseT) {
        StepVerifier.create(signResponseBes).expectNextCount(1).verifyComplete();
        StepVerifier.create(signResponseT).expectNextCount(1).verifyComplete();
    }

    private static void checkAssertionsBasedOnErrors(Mono<PnSignDocumentResponse> responseMono) {
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiPermanentErrorException).verify();
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        StepVerifier.create(responseMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
    }


    private void produceErrorResponse() throws JsonProcessingException {
        // Serialize error responses
        var badRequestError = new ServerErrorResponse(400, "Invalid file", UUID.randomUUID().toString());
        var genericError = new ServerErrorResponse(100, "Generic Error", UUID.randomUUID().toString());
        var overloadServer = new ServerErrorResponse(503, "Server overloaded", UUID.randomUUID().toString());
        var genericErrorBody = objectMapper.writeValueAsString(genericError);
        var overloadedServerErrorBody = objectMapper.writeValueAsString(overloadServer);
        var badRequestErrorBody = objectMapper.writeValueAsString(badRequestError);

        var unexpectedPayload = """
                {
                    "status": 501,
                    "detail": "Unexpected error"
                }
                """;

        // Mock responses
        MockResponse error400 = new MockResponse().setResponseCode(400).setBody(badRequestErrorBody); // Temporary error
        MockResponse error401 = new MockResponse().setResponseCode(401);                              // Permanent error
        MockResponse error500 = new MockResponse().setResponseCode(500).setBody(genericErrorBody);    // Temporary error
        MockResponse error501 = new MockResponse().setResponseCode(501);                              // Temporary error
        MockResponse error503 = new MockResponse().setResponseCode(503).setBody(unexpectedPayload);   // Temporary error
        MockResponse error503WithBody = new MockResponse().setResponseCode(503)                       // Temporary error
                .setBody(overloadedServerErrorBody);

        // Enqueue responses
        mockWebServer.enqueue(error400);
        mockWebServer.enqueue(error401);
        mockWebServer.enqueue(error500);
        mockWebServer.enqueue(error501);
        mockWebServer.enqueue(error503);
        mockWebServer.enqueue(error503WithBody);
    }


    private static MockResponse getOkResponse(byte[] bytes) {
        Buffer buffer = new Buffer().write(bytes);
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/octet-stream")
                .setHeader("X-SIGNBOX-TRANSACTION-ID", "123456")
                .setBody(buffer);
    }
}