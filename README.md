# pn-ss-namirial-sign

## Description

This is a library to sign document using PAdES, XAdES and CAdES format.

## Requirements
- Java 17
- Maven 3.9+

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

...

byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/in/sample.pdf"));
Mono<PnSignDocumentResponse> responseBes = signService
        .signPdfDocument(bytes, false)
        .flatMap(b -> consume signed content);
```

## Run tests

**On Windows**
```shell
set PnEcNamirialServerAddress=<api_endpoint>
mvn test -Dnamirial.server.apikey=<your_api_key>
```

**On Linux**
```shell
export PnEcNamirialServerAddress=<api_endpoint>
mvn test -Dnamirial.server.apikey=<your_api_key>
```

Then check the `src/testresources/out` folder for generated signed file.