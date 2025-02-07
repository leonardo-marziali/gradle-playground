package com.leomarzia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringWebDemoApplication {

    public static void main(String[] args) {

        SpringApplication.run(SpringWebDemoApplication.class, args);
    }

    @GetMapping("/")
    public ResponseEntity<String> home() {

        return ResponseEntity.ok("Hello and welcome!");
    }

}
