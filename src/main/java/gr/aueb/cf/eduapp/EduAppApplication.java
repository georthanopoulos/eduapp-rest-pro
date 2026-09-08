package gr.aueb.cf.eduapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods                // automated within springBoot 4. otherwise in springBoot 3 we should insert libraries in nuild gradle..and do @EnableReTry as well.
@EnableJpaAuditing
public class EduAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduAppApplication.class, args);
    }

}
