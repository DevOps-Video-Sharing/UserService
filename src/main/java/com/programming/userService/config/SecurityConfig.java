package com.programming.userService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collection;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity httpSecurity) throws Exception {
        JwtRequestFilter jwtRequestFilter = new JwtRequestFilter();
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/user/register", "/user/error").permitAll()
                        // .requestMatchers("/listUser").permitAll()
                        .requestMatchers("/user/login2").permitAll()
                        .requestMatchers("/user/login3").permitAll()
                        .requestMatchers("/user/listUserbyId/**").permitAll()
                        .requestMatchers("/user/hello-world").permitAll()
                        .requestMatchers("/user/send-verification-email").permitAll()
                        .requestMatchers("/user/logout").permitAll()
                        .requestMatchers("/user/listUserbyUsername").permitAll()
                        .requestMatchers("/user/listUserbyId/**").permitAll()
                        .requestMatchers("/user/updateProfile/**").permitAll()
                        .requestMatchers("/user/changePassword/**").permitAll()
                        .requestMatchers("/user/listUser").permitAll()

                        .requestMatchers("/user/").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}