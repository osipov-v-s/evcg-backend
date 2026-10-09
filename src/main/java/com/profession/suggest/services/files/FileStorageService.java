package com.profession.suggest.services.files;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class FileStorageService {
    @Value("${public.folder}")
    private String baseFolder;

    public String saveFile(MultipartFile file, String subfolder, boolean addPrefix) throws IOException {
        try{
            Path path = Paths.get(baseFolder, subfolder);
            Files.createDirectories(path);
            String filename = (addPrefix ? generateUniqueFilename(file.getOriginalFilename()) : file.getOriginalFilename());
            Path filePath = path.resolve(filename);

            file.transferTo(filePath.toFile());
            return "public/" + subfolder + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to save file to {}: {}", subfolder, e.getMessage());
            throw new IOException("Failed to save file to " + subfolder, e);
        }
    }
    public Boolean deleteFile(String filePath) throws IOException {
        try{
            String relativePath = filePath;
            if (relativePath.startsWith("public/")) {
                relativePath = relativePath.substring("public/".length());
            }
            // Now relativePath = "comparison/collections/3/1_xxx.png"

            //Build full path: baseFolder + "public" + relativePath
            Path fullPath = Paths.get(baseFolder, relativePath);

            return Files.deleteIfExists(fullPath);
        } catch (Exception e) {
            //throw new RegularException("Error while deleting file", HttpStatus.INTERNAL_SERVER_ERROR.value());
            System.err.println("Error while deleting file: " + e.getMessage());
            throw new IOException("Error while deleting file: " + e.getMessage());
        }
    }
    private String generateUniquePrefix() {
        long currentTimeMillis = System.currentTimeMillis();
        //int randomNumber = new Random().nextInt(1000);
        return String.valueOf(currentTimeMillis);
    }
    public String generateUniqueFilename(String filename) {
        String prefix = generateUniquePrefix();
        String baseName = filename.substring(0, filename.lastIndexOf("."));
        int lastDotIndex = filename.lastIndexOf(".");

        if (lastDotIndex == -1) {
            return filename + "_" + prefix;
        }

        String fileExtension = filename.substring(filename.lastIndexOf("."));
        return baseName + "_" + prefix + fileExtension;
    }
    public static String getStaticPath(String absolutePath){
        String path = Paths.get(absolutePath).getFileName().toString();
        return (!path.endsWith("/")) ? path+"/" : path;
    }
    public String extractFilename(String path){
        return path.replaceFirst(".*/", "");
    }
    public Path resolve(String relativeKey) {
        String clean = relativeKey.startsWith("public/")
                ? relativeKey.substring("public/".length())
                : relativeKey;
        return Paths.get(baseFolder).resolve(clean);
    }
}
