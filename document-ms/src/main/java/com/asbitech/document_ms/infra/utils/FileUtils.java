package com.asbitech.document_ms.infra.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import reactor.core.publisher.Mono;

public class FileUtils {
    public static Mono<Path> ensureFolderExists(String... folders) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get("", folders);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        });
    }

}
