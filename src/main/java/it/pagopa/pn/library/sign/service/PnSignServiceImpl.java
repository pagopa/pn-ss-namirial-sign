package it.pagopa.pn.library.sign.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.pojo.SignatureFormat;
import it.pagopa.pn.library.sign.pojo.SignatureLevel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class PnSignServiceImpl implements PnSignService {

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        var format = SignatureFormat.PADES;
        var level = timestamping ? SignatureLevel.TIMESTAMP : SignatureLevel.BASIC;
        var requestId = UUID.randomUUID().toString();
        log.info("Invoked [signPdfDocument] request {} with params format={}, level={}, requestBody length: {} bytes.", requestId, format, level, fileBytes.length);
        return SignServiceClient.sign(requestId, fileBytes, format, level);
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        String format = SignatureFormat.XADES;
        String level = timestamping ? SignatureLevel.TIMESTAMP : SignatureLevel.BASIC;
        var requestId = UUID.randomUUID().toString();
        log.info("Invoked [signXmlDocument] request {} with params format={}, level={}, requestBody length: {} bytes.", requestId, format, level, fileBytes.length);
        return SignServiceClient.sign(requestId, fileBytes, format, level);
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        String format = SignatureFormat.CADES;
        String level = timestamping ? SignatureLevel.TIMESTAMP : SignatureLevel.BASIC;
        var requestId = UUID.randomUUID().toString();
        log.info("Invoked [pkcs7Signature] request {} with params format={}, level={}, requestBody length: {} bytes.", requestId, format, level, fileBytes.length);
        return SignServiceClient.sign(requestId, fileBytes, format, level);
    }
}
