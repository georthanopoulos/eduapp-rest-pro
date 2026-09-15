package gr.aueb.cf.eduapp.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Stand-in for {@code EduAppApplication} used as the {@code @SpringBootConfiguration}
 * source for {@code @WebMvcTest} slices in this package.
 * <p>
 * Spring resolves the nearest {@code @SpringBootConfiguration} by walking up from the
 * test class's own package, so this one (living alongside the controller tests) is
 * found before the real {@code EduAppApplication}. That matters here because the real
 * application class also carries {@code @EnableJpaAuditing}, which eagerly registers a
 * bean requiring a real {@code EntityManagerFactory} - something a web-layer slice
 * test never configures, causing context startup to fail with
 * "JPA metamodel must not be empty".
 * <p>
 * {@code scanBasePackages} is used (instead of a bare {@code @ComponentScan}) so the
 * component scan keeps {@code @SpringBootApplication}'s built-in {@code TypeExcludeFilter}
 * wiring - without it, {@code @WebMvcTest}'s "controllers/filters/advice only" filtering
 * has nothing to attach to and every {@code @Service}/{@code @Repository} bean gets
 * scanned in too.
 */
@SpringBootApplication(scanBasePackages = "gr.aueb.cf.eduapp")
class WebLayerTestConfiguration {
}
