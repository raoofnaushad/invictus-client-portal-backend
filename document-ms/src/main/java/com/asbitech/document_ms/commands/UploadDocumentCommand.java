package com.asbitech.document_ms.commands;

import org.springframework.http.codec.multipart.FilePart;

import com.asbitech.common.domain.Command;

import lombok.Builder;

@Builder
public record UploadDocumentCommand (FilePart  file , String uploadedByUserId) implements Command {

    @Override
    public DocumentCommandType getCommandType() {
       return DocumentCommandType.UPLOAD_DOCUMENT;
    }  

}