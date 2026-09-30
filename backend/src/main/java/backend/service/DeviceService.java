package backend.service;

import backend.model.Device;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {

    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    public void register(Device device) {
        device.setOnline(true);
         System.out.println("Registering Device ID: " + device.getDeviceId());
        devices.put(device.getDeviceId(), device);
    }

    public Collection<Device> getDevices() {
        return devices.values();
    }

    public void remove(String deviceId) {
        devices.remove(deviceId);
    }
}