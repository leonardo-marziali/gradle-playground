package com.leomarzia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
public class SpringReactiveDemoApplication {

    public static void main(String[] args) {

        SpringApplication.run(SpringReactiveDemoApplication.class, args);
    }

    @GetMapping("/")
    public Mono<ResponseEntity<String>> home() {

        return Mono.just(ResponseEntity.ok("Hello and welcome!"));
    }

}
