package backend.repository;

import backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByOwnerEmail(String ownerEmail);

    Optional<Device> findByDeviceUUID(String deviceUUID);

}