package backend.controller;

import backend.dto.LoginRequest;
import backend.dto.LoginResponse;
import backend.entity.User;
import backend.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {

    System.out.println("REGISTER API HIT");

    return userService.register(user);
    
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
