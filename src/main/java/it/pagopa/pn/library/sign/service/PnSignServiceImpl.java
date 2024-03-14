package it.pagopa.pn.library.sign.service;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.pojo.SignatureFormat;
import it.pagopa.pn.library.sign.pojo.SignatureLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class PnSignServiceImpl implements PnSignService {

    private String apiEndpoint;


    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        return applySignature(SignatureFormat.PADES, timestamping, fileBytes);
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        return applySignature(SignatureFormat.XADES, timestamping, fileBytes);
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        return applySignature(SignatureFormat.CADES, timestamping, fileBytes);
    }

    private Mono<PnSignDocumentResponse> applySignature(String format, Boolean timestamping, byte[] fileBytes) {
        if(fileBytes == null || fileBytes.length == 0) {
            return Mono.error(new PnSpapiPermanentErrorException("fileBytes cannot be null or empty"));
        }
        String level = timestamping ? SignatureLevel.TIMESTAMP : SignatureLevel.BASIC;
        var requestId = UUID.randomUUID().toString();
        String message = "Invoked [sign{}] request {} with params format={}, level={}, requestBody length: {} bytes.";
        log.info(message, format, requestId, format, level, fileBytes.length);
        return SignServiceClient.sign(apiEndpoint, requestId, fileBytes, format, level);
    }

}
