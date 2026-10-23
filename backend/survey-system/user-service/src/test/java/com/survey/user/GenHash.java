package com.survey.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Publisher@2026");
        System.out.println(hash);
        System.out.println("matches = " + encoder.matches("Publisher@2026", hash));
    }
}