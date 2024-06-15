package com.programming.streaming.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.programming.streaming.entity.AuthUser;
import com.programming.streaming.repository.AuthUserRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender javaMailSender;

    static class Email {
        private String email;

        public String getEmail() {
            return email;
        }
    }
    @GetMapping("/)
    public String getServiceName(){
        return "User Service";
    }

    @PostMapping("/send-verification-email")
    public String sendVerificationEmail(@RequestBody String emailJson) {
        SimpleMailMessage message = new SimpleMailMessage();
        Gson gson = new Gson();
        Email emailObject = gson.fromJson(emailJson, Email.class);
        String email = emailObject.getEmail();
        message.setTo(email);
        message.setSubject("Xác thực đăng ký");
        message.setText("Xin chào, Bạn đã đăng ký tài khoản thành công!");

        try {
            javaMailSender.send(message);
            return "Email xác thực đã được gửi thành công";
        } catch (MailException e) {
            return "Gửi email xác thực thất bại: " + e.getMessage();
        }
    }

    @PostMapping("/register")
    public ResponseEntity registerUser(@RequestBody AuthUser user) {
        try {
            if (userRepository.findByUsername(user.getUsername()).isPresent())
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken. Please try again");
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setFirstName(user.getFirstName());
            user.setLastName(user.getLastName());
            user.setTimestamp(new Date());
            user.setAvatar(getDefaultAvatar());
            AuthUser save = userRepository.save(user);
            return ResponseEntity.ok(save);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private byte[] getDefaultAvatar() throws IOException {
        String defaultAvatarPath = "/app/images/avatar.png"; // Path inside the Docker container
        Path path = Paths.get(defaultAvatarPath);
        return Files.readAllBytes(path);
    }

    @PostMapping("/login2")
    public ResponseEntity loginUser(@RequestBody AuthUser user) {
        try {
            AuthUser userFromDb = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            if (passwordEncoder.matches(user.getPassword(), userFromDb.getPassword())) {
                return ResponseEntity.ok(userFromDb);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity logoutUser() {
        try {
            return ResponseEntity.ok(HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/listUser")
    public ResponseEntity listUser() {
        try {
            return ResponseEntity.ok(userRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/listUserbyUsername")
    public ResponseEntity listUserbyUsername(@RequestBody AuthUser user) {
        try {
            return ResponseEntity.ok(userRepository.findByUsername(user.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/listUserbyId/{id}")
    public ResponseEntity listUserbyId(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(userRepository.findById(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/updateProfile/{id}")
    public ResponseEntity updateProfile(@PathVariable("id") String id, @RequestBody AuthUser user) {
        try {
            AuthUser userFromDb = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            userFromDb.setFirstName(user.getFirstName());
            userFromDb.setLastName(user.getLastName());
            AuthUser save = userRepository.save(userFromDb);

            return ResponseEntity.ok(HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/changePassword/{id}")
    public ResponseEntity changePassword(@PathVariable("id") String id,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            AuthUser userFromDb = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), userFromDb.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current password is incorrect");
            }

            userFromDb.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
            AuthUser save = userRepository.save(userFromDb);

            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
