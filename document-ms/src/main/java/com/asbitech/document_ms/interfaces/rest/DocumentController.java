package com.asbitech.document_ms.interfaces.rest;

import org.springframework.web.bind.annotation.RestController;

import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.PageResponse;
import com.asbitech.document_ms.application.DocumentCommandManager;
import com.asbitech.document_ms.commands.UpdateDocumentCommand;
import com.asbitech.document_ms.commands.UploadDocumentCommand;
import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.repository.DocumentRepo;
import com.asbitech.document_ms.domain.vo.DocumentId;
import com.asbitech.document_ms.domain.vo.DocumentStatus;
import com.asbitech.document_ms.interfaces.rest.dto.UpdateDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentCommandManager documentService;
    private final DocumentRepo documentRepo;
    ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_ID_HEADER = "X-User-ID";

    public DocumentController(DocumentCommandManager documentService,  DocumentRepo documentRepo) {
        this.documentService = documentService;
        this.documentRepo = documentRepo;

    }

    @GetMapping
    public Mono<ResponseEntity<?>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir, 
            @RequestHeader(USER_ID_HEADER) String userId) {
        LOG.info(userId); 
        return documentRepo.loadByClientId(userId, page, size)
                .flatMap(response -> {
                    if (response.isRight()) {


                        PageResponse<Document> pageResponse = response.get();

                        // Populate extractedData for each Document
                        pageResponse.getContent().forEach(document -> {
                            String docPathStr = document.getFilePath();
                            if (docPathStr != null) {
                                Path jsonPath = Path.of(docPathStr).resolveSibling(
                                        Path.of(docPathStr).getFileName().toString().replaceAll("\\.[^.]+$", ".json")
                                );

                                File jsonFile = jsonPath.toFile();
                                if (jsonFile.exists()) {
                                    try {
                                        Map<String, Object> map = objectMapper.readValue(jsonFile, Map.class);
                                        document.setExtractedData(map);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        document.setExtractedData(Map.of());
                                        if (document.getDocumentStatus().equals(DocumentStatus.Processing)) {
                                            document.setDocumentStatus(DocumentStatus.Pending);
                                        }
                                    }
                                } else {
                                    document.setExtractedData(Map.of());
                                    if (document.getDocumentStatus().equals(DocumentStatus.Processing)) {
                                            document.setDocumentStatus(DocumentStatus.Pending);
                                    }
                                }
                            }
                        });

                    return Mono.just(ResponseEntity.status(HttpStatus.OK).body(pageResponse));
                    } else {
                        CustomError customError = response.getLeft();
                        LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                        LOG.error("Error creating principal: {}", customError.getMessage());

                        return Mono.just(ResponseEntity
                                .status(customError.getHttpStatusCode()) // Return actual error status
                                .body(customError)); // Return CustomError as response body
                    }
                });
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<?>> uploadDocument(
            @RequestPart("file") FilePart file, @RequestHeader(USER_ID_HEADER) String userId) {
        LOG.info(userId); 
        return documentService.processCommand(UploadDocumentCommand.builder()
                .file(file).uploadedByUserId(userId).build())
                    .flatMap(response -> {
                        if (response.isRight()) {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.OK) 
                                    .body(response.get()));
                        } else {
                            CustomError customError = response.getLeft();
                            LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                            LOG.error("Error creating principal: {}", customError.getMessage());

                            return Mono.just(ResponseEntity
                                    .status(customError.getHttpStatusCode()) // Return actual error status
                                    .body(customError)); // Return CustomError as response body
                        }
                    });
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> updateDocument(@PathVariable("id") String id,
            @RequestBody UpdateDocumentRequest body, @RequestHeader(USER_ID_HEADER) String userId) {
        LOG.info(userId); 
        return documentService.processCommand(UpdateDocumentCommand.builder()
                .documentId(new DocumentId(id)).documentStatus(body.getDocumentStatus()).extractedDdata(body.getExtractedData()).build())
                    .flatMap(response -> {
                        if (response.isRight()) {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.OK) 
                                    .body(response.get()));
                        } else {
                            CustomError customError = response.getLeft();
                            LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                            LOG.error("Error creating principal: {}", customError.getMessage());

                            return Mono.just(ResponseEntity
                                    .status(customError.getHttpStatusCode()) // Return actual error status
                                    .body(customError)); // Return CustomError as response body
                        }
                    });
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getDocumentById(@PathVariable String id) {
        return documentRepo.loadById(id) // assume this returns Mono<Either<CustomError, Document>>
                .flatMap(response -> {
                    if (response.isRight()) {
                        Document document = response.get();

                        // populate extractedData if JSON exists
                        String docPathStr = document.getFilePath();
                        if (docPathStr != null) {
                            Path jsonPath = Path.of(docPathStr).resolveSibling(
                                    Path.of(docPathStr).getFileName().toString().replaceAll("\\.[^.]+$", ".json")
                            );

                            File jsonFile = jsonPath.toFile();
                            if (jsonFile.exists()) {
                                try {
                                    Map<String, Object> map = objectMapper.readValue(jsonFile, Map.class);
                                    document.setExtractedData(map);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    document.setDocumentStatus(DocumentStatus.Pending);// fallback empty map
                                    document.setExtractedData(Map.of()); // fallback empty map
                                }
                            } else {
                                document.setExtractedData(Map.of()); // no JSON found
                                document.setDocumentStatus(DocumentStatus.Pending);// fallback empty map
                            }
                        }

                        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(document));
                    } else {
                        CustomError customError = response.getLeft();
                        return Mono.just(ResponseEntity
                                .status(customError.getHttpStatusCode())
                                .body(customError));
                    }
                });
    }
}
