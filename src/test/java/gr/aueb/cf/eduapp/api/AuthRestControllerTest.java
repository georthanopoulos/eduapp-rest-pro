package gr.aueb.cf.eduapp.api;

import gr.aueb.cf.eduapp.authentication.AuthenticationService;
import gr.aueb.cf.eduapp.authentication.JwtService;
import gr.aueb.cf.eduapp.dto.AuthenticationRequestDTO;
import gr.aueb.cf.eduapp.dto.AuthenticationResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link AuthRestController}. The Spring Security
 * filter chain is disabled ({@code addFilters = false}) since the endpoint
 * under test is unauthenticated; {@link AuthenticationService} is mocked so
 * these tests exercise only request mapping and error translation.
 */
@WebMvcTest(AuthRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    // required to satisfy JwtAuthenticationFilter's constructor dependencies;
    // the filter itself never runs since addFilters = false above.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void authenticate_returns200WithToken_whenCredentialsAreValid() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("jdoe", "Passw0rd!");
        when(authenticationService.authenticate(any(AuthenticationRequestDTO.class)))
                .thenReturn(new AuthenticationResponseDTO("jwt-token-value"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"));
    }

    @Test
    void authenticate_returns401_whenCredentialsAreInvalid() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("jdoe", "wrong-password");
        when(authenticationService.authenticate(any(AuthenticationRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void authenticate_returns401_whenAccountIsDisabled() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("jdoe", "Passw0rd!");
        when(authenticationService.authenticate(any(AuthenticationRequestDTO.class)))
                .thenThrow(new DisabledException("Account disabled"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }
}
