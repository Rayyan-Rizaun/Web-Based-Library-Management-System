package com.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Feature slices live in their own {@code com.lms.<feature>}
 * packages and are picked up automatically by component scanning rooted
 * here — see CLAUDE.md for the package-by-feature convention and the
 * feature ownership table.
 */
@SpringBootApplication
public class LibrarySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarySystemApplication.class, args);
    }
}
