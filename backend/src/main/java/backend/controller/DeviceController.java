package backend.controller;

import backend.model.Device;
import backend.service.DeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/device")
@CrossOrigin("*")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public Device register(@RequestBody Device device) {
        deviceService.register(device);
        return device;
    }

    @GetMapping("/list")
    public Collection<Device> getDevices() {
        return deviceService.getDevices();
    }
}