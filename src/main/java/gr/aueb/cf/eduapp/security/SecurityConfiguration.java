package gr.aueb.cf.eduapp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Value("${allowed.origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider) throws Exception {

        http
                .cors(httpSecurityCorsConfigurer ->
                        httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)     // this line only matters in Server Size Rendering (SSR). Otherwise, it must be disabled!!!
                                                           // The csrf does not exist in CSR way.
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/authenticate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/teachers").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/teachers/{uuid}/*").permitAll()
//                        .requestMatchers("/api/v1/eligible/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").hasAuthority("VIEW_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/teachers/{uuid}").hasAuthority("EDIT_TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/teachers").hasAuthority("VIEW_TEACHERS")
                        .requestMatchers(HttpMethod.GET, "/api/v1/teachers/{uuid}").hasAnyAuthority("VIEW_TEACHER", "VIEW_ONLY_TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/teachers/{uuid}").hasAuthority("DELETE_TEACHER")
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))    // this line indicates that this app uses token instead of cookies, something which is expected as this is a CSR app.
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)    //add this filter prior to authorization filter.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);             // double * -> in order for all URLs to pass through CORS

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);   // Dao -> creates username and password. does all the job!
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);            // basic one way hashing algorithm, one of the two basic ones!
    }                                                   // best practice to set 12 rounds. for safety purposes preventing from brute force attack.
}
