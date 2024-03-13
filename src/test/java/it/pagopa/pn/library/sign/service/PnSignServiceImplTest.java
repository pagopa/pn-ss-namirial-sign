package it.pagopa.pn.library.sign.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.pojo.SignatureFormat;
import it.pagopa.pn.library.sign.pojo.SignatureLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;

@Slf4j
public class PnSignServiceImplTest {

    private static PnSignService signService;

    @BeforeAll
    static void beforeAll() {
        signService = new PnSignServiceImpl();
    }

    @Test
    @DisplayName("Sign a PDF file using PADES format and BES and T level")
    void testSignPdfDocument() throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
        Mono<PnSignDocumentResponse> responseBes = signService.signPdfDocument(bytes, false)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.PADES, SignatureLevel.BASIC, b.getSignedDocument())));
        StepVerifier.create(responseBes).expectNextCount(1).verifyComplete();

        Mono<PnSignDocumentResponse> responseT = signService.signPdfDocument(bytes, true)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.PADES, SignatureLevel.TIMESTAMP, b.getSignedDocument())));
        StepVerifier.create(responseT).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Sign a XML file using XADES format and BES and T level")
    void testSignXmlDocument() throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.xml"));

        Mono<PnSignDocumentResponse> signResponseBes = signService.signXmlDocument(bytes, false)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.XADES, SignatureLevel.BASIC, b.getSignedDocument())));
        StepVerifier.create(signResponseBes).expectNextCount(1).verifyComplete();

        Mono<PnSignDocumentResponse> signResponseT = signService.signXmlDocument(bytes, true)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.XADES, SignatureLevel.TIMESTAMP, b.getSignedDocument())));
        StepVerifier.create(signResponseT).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("Sign a PDF file using CADES format and BES and T level")
    void testSignPck7Document() throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));

        Mono<PnSignDocumentResponse> signResponseBes = signService.pkcs7Signature(bytes, false)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.CADES, SignatureLevel.BASIC, b.getSignedDocument())));
        StepVerifier.create(signResponseBes).expectNextCount(1).verifyComplete();

        Mono<PnSignDocumentResponse> signResponseT = signService.pkcs7Signature(bytes, true)
                .flatMap(b -> Mono.fromCallable(() -> writeSignedDocument(SignatureFormat.CADES, SignatureLevel.TIMESTAMP, b.getSignedDocument())));
        StepVerifier.create(signResponseT).expectNextCount(1).verifyComplete();
    }

    private PnSignDocumentResponse writeSignedDocument(String format, String level, byte[] signedDocument) {
        try {
            FileUtils.writeByteArrayToFile(new File("src/test/resources/out/signed-%s-%s.%s".formatted(format,level, getExtensionFromFormat(format))), signedDocument);
        } catch (IOException ignored) {
            log.error("Error writing signed document: {}", ignored.getMessage());
        }
        return new PnSignDocumentResponse(signedDocument);
    }

    private String getExtensionFromFormat(String format) {
        return switch (format) {
            case SignatureFormat.CADES -> "p7m";
            case SignatureFormat.PADES -> "pdf";
            case SignatureFormat.XADES -> "xml";
            default -> "dat";
        };
    }
}