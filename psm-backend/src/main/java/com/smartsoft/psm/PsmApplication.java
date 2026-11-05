package com.smartsoft.psm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // l'envoi d'email se fait en arrière-plan, sans bloquer l'inscription
public class PsmApplication {
    public static void main(String[] args) {
        SpringApplication.run(PsmApplication.class, args);
    }
}
