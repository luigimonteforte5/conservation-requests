package com.luigimonteforte.conservationrequests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ConservationRequestsApplication {

    static void main(String[] args) {
        SpringApplication.run(ConservationRequestsApplication.class, args);
    }

}
