package com.devisri.finance;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Logins use our own JWT tokens, so Spring's built-in default user (and its printed password) is not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableMongoAuditing
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }

    /** One shared clock so tests can pretend "today" is any date they like. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
