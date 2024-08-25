package com.taskmanager.controller;

import com.taskmanager.domain.JwpToken;
import com.taskmanager.domain.User;
import com.taskmanager.domain.enums.Role;
import com.taskmanager.domain.enums.TokenType;
import com.taskmanager.dto.UserDTO;
import com.taskmanager.security.JwtUtil;
import com.taskmanager.service.JwtService;
import com.taskmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.taskmanager.controller.Utils.*;
import static com.taskmanager.controller.Utils.getUserDTO;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        if (userService.findByEmail(user.getEmail()) != null) {
            return createErrorResponse("Email is already in use.", HttpStatus.CONFLICT);
        }
        User userDB = userService.saveUser(user);

        UserDTO userDTO = new UserDTO(userDB.getId(), userDB.getFirstName(), userDB.getLastName(), userDB.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        User userDB = userService.findByEmail(user.getEmail());
        if (userDB == null) {
            return createErrorResponse("User not found.", HttpStatus.NOT_FOUND);
        }

        if (!user.getPassword().equals(userDB.getPassword())) {
            return createErrorResponse("Invalid password.", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user);
        HashMap<String, Object> claims = new HashMap<>();
        String refreshToken = jwtUtil.generateRefreshToken(claims, user);

        JwpToken accessToken = new JwpToken(token, TokenType.ACCESS, new Date(),
                new Date(System.currentTimeMillis() + JwtUtil.ACCESS_EXPIRATION_TIME), userDB);

        JwpToken refreshTk  = new JwpToken(refreshToken, TokenType.REFRESH, new Date(),
                new Date(System.currentTimeMillis() + JwtUtil.REFRESH_EXPIRATION_TIME), userDB);
        jwtService.saveJwpToken(accessToken);
        jwtService.saveJwpToken(refreshTk);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return createErrorResponse("Refresh token is missing.", HttpStatus.NOT_FOUND);
        }


        String newAccessToken = jwtService.refreshToken(refreshToken);
        if (newAccessToken == null) {
            return createErrorResponse("Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED);
        }

        Map<String, String> response = new HashMap<>();
        response.put("token", newAccessToken);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return getEntityResponse(id, userService::findById, Utils::getUserDTO, "User");
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        String currentUsername = getCurrentUsername();

        User currentUser = userService.findByEmail(currentUsername);
        User userToUpdate = userService.findById(id);

        if (userToUpdate == null) {
            return createErrorResponse("User not found.", HttpStatus.NOT_FOUND);
        }

        if (!currentUsername.equals(userToUpdate.getEmail()) && !currentUser.getRoles().contains(Role.ADMIN)) {
            return createErrorResponse("You do not have permission to update this user.", HttpStatus.FORBIDDEN);
        }

        updateFieldIfNotNull(userToUpdate::setFirstName, user.getFirstName());
        updateFieldIfNotNull(userToUpdate::setLastName, user.getLastName());
        updateFieldIfNotNull(userToUpdate::setEmail, user.getEmail());
        updateFieldIfNotNull(userToUpdate::setPassword, user.getPassword());
        updateFieldIfNotNull(userToUpdate::setActive, user.isActive());

        return ResponseEntity.ok(getUserDTO(userService.saveUser(userToUpdate)));
    }
}

