package com.asbitech.document_ms.commands.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.asbitech.common.domain.CommandHandler;
import com.asbitech.document_ms.commands.UpdateDocumentCommand;
import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.repository.DocumentRepo;
import com.asbitech.document_ms.domain.vo.DocumentStatus;
import com.asbitech.document_ms.events.DocumentUpdateEvent;
import com.asbitech.document_ms.events.DocumentUpdatedEvent;
import com.asbitech.document_ms.infra.externel.ProcessDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;


@Component
public class UpdateDocumentCommandHandler implements CommandHandler<UpdateDocumentCommand, DocumentUpdateEvent, Document> {
    private final DocumentRepo documentRepo;
    ObjectMapper objectMapper = new ObjectMapper();

    public UpdateDocumentCommandHandler(DocumentRepo documentRepo) {
        this.documentRepo = documentRepo;
    }

    @Override
    public Mono<DocumentUpdateEvent> handle(UpdateDocumentCommand command, Document mainEntity) {
        mainEntity.setDocumentStatus(command.documentStatus());
        mainEntity.setUpdatedAt(LocalDateTime.now());

        String docPathStr = mainEntity.getFilePath();
        if (docPathStr != null && command.extractedDdata() != null && !command.extractedDdata().isEmpty()) {
            Path jsonPath = Path.of(docPathStr).resolveSibling(
                    Path.of(docPathStr).getFileName().toString().replaceAll("\\.[^.]+$", ".json")
            );

            File jsonFile = jsonPath.toFile();
            if (jsonFile.exists()) {
                try {
                    Map<String, Object> map = objectMapper.readValue(jsonFile, Map.class);
                    mainEntity.setDocumentType((String)map.get("category"));
                    map.put("extracted_data", command.extractedDdata());
                    objectMapper.writeValue(jsonFile, map);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("JSON file does not exist at path: " + jsonPath.toString());
            }
        }

        System.out.println("Updated Document: " + mainEntity);

        return documentRepo.store(mainEntity)
                .flatMap(updatedDocEith -> updatedDocEith.fold(
                        error -> {
                            System.err.println("Error updating document: " + error);
                            
                            return Mono.error(new RuntimeException("Failed to update document"));
                        },
                        updatedDoc -> Mono.just(DocumentUpdatedEvent.builder()
                                .id(updatedDoc.getId())
                                .build())
                ));
    }
    
}