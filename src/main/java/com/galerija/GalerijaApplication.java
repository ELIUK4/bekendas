package com.galerija;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.galerija")
public class GalerijaApplication {
    public static void main(String[] args) {
        SpringApplication.run(GalerijaApplication.class, args);
    }
}
