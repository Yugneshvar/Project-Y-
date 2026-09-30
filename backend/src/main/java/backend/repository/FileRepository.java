package backend.repository;

import backend.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByUserEmail(String userEmail);

    Optional<FileEntity> findByFileNameAndUserEmail(String fileName, String userEmail);
}
