package com.programming.userService.controller;

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
import com.programming.userService.entity.AuthUser;
import com.programming.userService.repository.AuthUserRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.programming.userService.util.JwtUtil;
import com.programming.userService.entity.CustomUserDetails;
import com.programming.userService.entity.AuthUser;
import org.springframework.data.redis.core.RedisTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_CACHE = "USER_CACHE";

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JavaMailSender javaMailSender;

    static class Email {
        private String email;

        public String getEmail() {
            return email;
        }
    }
    @GetMapping("/")
    public String getServiceName(){
        //ELK 
        MDC.put("type", "userservice");
        logger.info("User Service Start");
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
            AuthUser userFromDb = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            MDC.put("type", "userservice");
            MDC.put("action", "register");
            logger.info("UserID: " + userFromDb.getId());
            return ResponseEntity.ok(save);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private byte[] getDefaultAvatar() throws IOException {
        String defaultAvatarPath = "/app/images/avatar.png"; // Path inside the Docker container
        //String defaultAvatarPath = "src/main/java/com/programming/userService/images/avatar.png"; // Path on local machine
        Path path = Paths.get(defaultAvatarPath);
        return Files.readAllBytes(path);
    }

    @PostMapping("/login3")
    public ResponseEntity<?> loginUser3(@RequestBody AuthUser user) {
        try {
            AuthUser userFromDb = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));

            if (passwordEncoder.matches(user.getPassword(), userFromDb.getPassword())) {
                // Chuyển AuthUser sang CustomUserDetails
                CustomUserDetails userDetails = new CustomUserDetails(userFromDb);
                String token = jwtUtil.generateToken(userDetails); // Tạo JWT token với UserDetails
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
    
    @PostMapping("/login2")
    public ResponseEntity loginUser2(@RequestBody AuthUser user) {
        try {
            AuthUser userFromDb = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            if (passwordEncoder.matches(user.getPassword(), userFromDb.getPassword())) {
                MDC.put("type", "userservice");
                MDC.put("action", "login");
                logger.info("UserID: " + userFromDb.getId());
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

    @Cacheable(value = USER_CACHE, key = "#id")
    @GetMapping("/listUserbyId/{id}")
    public ResponseEntity listUserbyId(@PathVariable("id") String id) {
        try {
            // Attempt to fetch user from Redis cache
            AuthUser cachedUser = (AuthUser) redisTemplate.opsForHash().get(USER_CACHE, id);

            if (cachedUser != null) {
                // If user is found in Redis, return it
                return ResponseEntity.ok(cachedUser);
            }

            // If user is not found in Redis, fetch from MongoDB
            AuthUser user = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            // Cache the user in Redis
            redisTemplate.opsForHash().put(USER_CACHE, id, user);

            // Return the user data
            return ResponseEntity.ok(user);
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
            String tempUserId = userFromDb.getId();

            MDC.put("type", "userservice");
            MDC.put("action", "update-profile");
            logger.info("UserID: " + userFromDb.getId());
            
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

            MDC.put("type", "userservice");
            MDC.put("action", "change-password");
            logger.info("UserID: " + userFromDb.getId());

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
