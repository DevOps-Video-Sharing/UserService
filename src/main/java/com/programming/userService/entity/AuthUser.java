package com.programming.userService.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.io.Serializable;

@Data
@Builder
@Document("user")
public class AuthUser implements Serializable {
    @Id
    private String id;
    @Indexed
    private String username;
    private String password;
    private boolean active;
    private String firstName;
    private String lastName;
    private String sub;
    private byte[] avatar;
    private String email;
    private Date timestamp;


    private static final long serialVersionUID = 1L;
}
