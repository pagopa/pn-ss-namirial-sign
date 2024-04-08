# pn-ss-namirial-sign

## Description

This is a library to sign document using PAdES, XAdES and CAdES format.

## Requirements
- Java 17
- Maven 3.8.1

## Configuration

The library uses the following properties:
- `namirial.server.address`: the URL of the Namirial Sign service
- `namirial.server.apikey`: the API key to access the Namirial Sign service
- `namirial.server.max-connections`: the number of maximum connections to the Namirial Sign service (default: 40)
- `namirial.server.pending-acquire-timeout`: the timeout in seconds to acquire a connection to the Namirial Sign service (default: 600)

## Usage

**Import the library in your project**
```
<dependency>
    <groupId>it.pagopa.pn</groupId>
    <artifactId>pn-ss-namirial-sign</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Use the sign client**
```java
PnSignService signService = new PnSignServiceImpl();

// Load content from file
byte[] bytes = ...
Mono<PnSignDocumentResponse> responseBes = signService
        .signPdfDocument(bytes, false).flatMap(b -> consume signed content);
```

## Build and install
```shell
./mvnw clean install
```

## Run tests
```shell
./mvnw test
```