package backend.controller;

import backend.entity.FileEntity;
import backend.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             Principal principal) {

        try {
            String fileName = safeFileName(file.getOriginalFilename());
            if (fileName == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("A non-empty file with a valid name is required.");
            }
            if (fileService.findByFileName(fileName, principal.getName()) != null) {
                return ResponseEntity.status(409).body("A file with this name already exists.");
            }

            Path uploadPath = userUploadPath(principal.getName());
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.getParent().equals(uploadPath.normalize())) {
                return ResponseEntity.badRequest().body("Invalid file name.");
            }

            Files.copy(
                    file.getInputStream(),
                    filePath
            );

            fileService.saveFile(
                    fileName,
                    file.getContentType(),
                    file.getSize(),
                    principal.getName()
            );

            return ResponseEntity.ok("File uploaded successfully!");

        } catch (IOException e) {

            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());

        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, Principal principal) {

        try {
            FileEntity file = fileService.findByFileName(fileName, principal.getName());
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = userUploadPath(principal.getName()).resolve(file.getFileName()).normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName, Principal principal) {

        try {
            FileEntity file = fileService.findByFileName(fileName, principal.getName());
            if (file == null) {
                return ResponseEntity.status(404).body("File not found!");
            }
            Path filePath = userUploadPath(principal.getName()).resolve(file.getFileName()).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(404).body("File not found!");
            }

            Files.delete(filePath);
            fileService.delete(file);

            return ResponseEntity.ok("File deleted successfully!");

        } catch (IOException e) {

            return ResponseEntity.badRequest()
                    .body("Delete failed: " + e.getMessage());

        }
    }

    @GetMapping
    public List<FileEntity> getMyFiles(Principal principal) {
        return fileService.getMyFiles(principal.getName());
    }

    private Path userUploadPath(String userEmail) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userEmail.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Paths.get(UPLOAD_DIR, HexFormat.of().formatHex(digest)).toAbsolutePath().normalize();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return null;
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return fileName.isBlank() || fileName.equals(".") || fileName.equals("..") ? null : fileName;
    }
}
