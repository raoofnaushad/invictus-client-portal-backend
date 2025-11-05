package com.asbitech.document_ms.commands.handlers;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.asbitech.common.domain.CommandHandler;
import com.asbitech.common.domain.CustomError;
import com.asbitech.document_ms.commands.UploadDocumentCommand;
import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.repository.DocumentRepo;
import com.asbitech.document_ms.domain.vo.DocumentId;
import com.asbitech.document_ms.domain.vo.DocumentStatus;
import com.asbitech.document_ms.events.DocumentUploadEvent;
import com.asbitech.document_ms.events.DocumentUploadFailedEvent;
import com.asbitech.document_ms.events.DocumentUploadedEvent;
import com.asbitech.document_ms.infra.externel.ProcessDocumentService;
import com.asbitech.document_ms.infra.utils.FileUtils;

import reactor.core.publisher.Mono;

@Component
public class UploadDocumentCommandHandler implements CommandHandler<UploadDocumentCommand, DocumentUploadEvent, Document> {

    private final DocumentRepo documentRepo;
    private final ProcessDocumentService processDocumentService;

    public UploadDocumentCommandHandler( ProcessDocumentService processDocumentService, DocumentRepo documentRepo) {
        this.documentRepo = documentRepo;
        this.processDocumentService = processDocumentService;
    }

    @Override
    public Mono<DocumentUploadEvent> handle(UploadDocumentCommand command, Document document) {
        String filename = command.file().filename();
        


        return documentRepo.findByPath(filename)
                .flatMap(eitherDoc -> eitherDoc.fold(
                        leftError -> FileUtils.ensureFolderExists("uploads", command.uploadedByUserId())
                            .flatMap(folderPath -> {
                                Path filePath = folderPath.resolve(filename);

                                return command.file().transferTo(filePath)
                                        .then(Mono.defer(() -> {
                                                document.setFilePath(filePath.toString());
                                                document.setName(filename);
                                                document.setDocumentStatus(DocumentStatus.Processing);
                                                document.setClientId(command.uploadedByUserId());
                                                document.setId(new DocumentId());
                                                document.setCreatedAt(LocalDateTime.now());
                                                document.setUpdatedAt(LocalDateTime.now());
                                                document.setIsNew(true); // Mark as new for insert
                                                return documentRepo.store(document);
                                            }))
                                        .map(savedDocEith -> savedDocEith.fold(
                                            error -> (DocumentUploadEvent) DocumentUploadFailedEvent.builder()
                                                    .uploadedByUserId(command.uploadedByUserId())
                                                    .filename(filename)
                                                    .error(error)
                                                    .build(),
                                            savedDoc -> {
                                                processDocumentService.processDocument(filePath.toString()).subscribe();

                                                return (DocumentUploadEvent) DocumentUploadedEvent.builder()
                                                .id(savedDoc.getId())
                                                .uploadedByUserId(command.uploadedByUserId())
                                                .filename(filename)
                                                .build();
                                            }
                                        ));
                            }),
                        existingDoc -> Mono.just((DocumentUploadEvent) DocumentUploadFailedEvent.builder()
                                .uploadedByUserId(command.uploadedByUserId())
                                .filename(filename)
                                .error(new CustomError(
                                        HttpStatus.CONFLICT,
                                        "DUPLICATE",
                                        "A document with the same filename already exists",
                                        null
                                    ))
                                .build())
                ))
                .onErrorResume(ex -> Mono.just((DocumentUploadEvent) DocumentUploadFailedEvent.builder()
                                .uploadedByUserId(command.uploadedByUserId())
                                .filename(filename)
                                .error(new CustomError(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "INTERNAL SERVER ERROR",
                                        ex.getMessage(),
                                        null
                                    ))
                                .build()
                ));
        }
}
