package backend.service;

import backend.entity.FileEntity;
import backend.repository.FileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileService {

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public FileEntity saveFile(String fileName, String fileType, Long fileSize, String userEmail) {

        FileEntity file = new FileEntity();

        file.setFileName(fileName);
        file.setFileType(fileType);
        file.setFileSize(fileSize);
        file.setUploadTime(LocalDateTime.now());
        file.setUserEmail(userEmail);

        return fileRepository.save(file);
    }

    public List<FileEntity> getMyFiles(String userEmail) {
        return fileRepository.findByUserEmail(userEmail);
    }

    public void delete(FileEntity file) {
        fileRepository.delete(file);
    }

    public FileEntity findByFileName(String fileName, String userEmail) {
        return fileRepository.findByFileNameAndUserEmail(fileName, userEmail).orElse(null);
    }
}
