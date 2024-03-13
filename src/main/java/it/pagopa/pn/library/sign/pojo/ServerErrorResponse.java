package it.pagopa.pn.library.sign.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class ServerErrorResponse {
    @JsonProperty("error_code")
    Integer errorCode;
    @JsonProperty("detail")
    String detail;
    @JsonProperty("transaction_id")
    String transactionId;
}
