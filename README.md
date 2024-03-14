# pn-ss-namirial-sign

## Description

This is a library to sign document using PAdES, XAdES and CAdES format.

## Requirements
- Java 17
- Maven 3.8.1

## Configuration

The library uses the following environment variables:
- `PnEcNamirialServerAddress`: the URL of the Namirial Sign service

The library uses the following properties:
- `namirial.server.apikey`: the API key to access the Namirial Sign service

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