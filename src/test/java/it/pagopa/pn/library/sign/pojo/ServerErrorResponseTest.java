package it.pagopa.pn.library.sign.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerErrorResponseTest {

    @Test
    @DisplayName("Test ServerErrorResponse")
    void testServerErrorResponse() {
        ServerErrorResponse serverErrorResponse = new ServerErrorResponse(1, "Detail", "TransactionId");
        var expected = """
                {
                  "error_code" : 1,
                  "detail" : "Detail",
                  "transaction_id" : "TransactionId"
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper();
        ServerErrorResponse uat = null;
        try {
            uat = objectMapper.readValue(expected, ServerErrorResponse.class);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        assertEquals(serverErrorResponse, uat);
    }

}