package org.link.linkvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkVaultApplication.class, args);
    }
}
