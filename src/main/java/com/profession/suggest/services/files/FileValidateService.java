package com.profession.suggest.services.files;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class FileValidateService {

    public List<String> findValidFolders(Path root, Set<String> requiredFiles) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> dirs = Files.list(root)){
            return dirs
                    .filter(Files::isDirectory)
                    .filter(dir -> hasAllRequired(dir, requiredFiles))
                    .map(dir -> dir.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
    private boolean hasAllRequired(Path dir, Set<String> required) {
        return required.stream()
                .allMatch(f -> Files.isRegularFile(dir.resolve(f)));
    }
}
