package gr.aueb.cf.eduapp.api;

import gr.aueb.cf.eduapp.authentication.JwtService;
import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.core.exceptions.FileUploadException;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.service.ITeacherService;
import gr.aueb.cf.eduapp.validator.TeacherInsertValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link TeacherRestController}. The Spring Security
 * filter chain is disabled ({@code addFilters = false}) so these tests focus
 * purely on request mapping, bean validation and error translation, with the
 * service layer and the custom validator replaced by Mockito mocks.
 */
@WebMvcTest(TeacherRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ITeacherService teacherService;

    @MockitoBean
    private TeacherInsertValidator teacherInsertValidator;

    // required to satisfy JwtAuthenticationFilter's constructor dependencies;
    // the filter itself never runs since addFilters = false above.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------- helpers ----------

    private TeacherInsertDTO validInsertDTO() {
        return TeacherInsertDTO.builder()
                .firstname("John")
                .lastname("Doe")
                .vat("123456789")
                .regionId(1L)
                .userInsertDTO(UserInsertDTO.builder()
                        .username("jdoe")
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoInsertDTO(PersonalInfoInsertDTO.builder()
                        .amka("12345678901")
                        .identityNumber("AB123456")
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }

    private TeacherUpdateDTO validUpdateDTO(UUID uuid) {
        return TeacherUpdateDTO.builder()
                .uuid(uuid)
                .firstname("Johnny")
                .lastname("Doeson")
                .vat("123456789")
                .regionId(1L)
                .userUpdateDTO(UserInsertDTO.builder()
                        .username("jdoe")
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("12345678901")
                        .identityNumber("AB123456")
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }

    // ---------- POST /api/v1/teachers ----------

    @Test
    void insertTeacher_returns201WithLocationHeader_whenRequestIsValid() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherReadOnlyDTO responseDTO = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");
        when(teacherService.saveTeacher(any(TeacherInsertDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInsertDTO())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(uuid.toString())))
                .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.firstname").value("John"))
                .andExpect(jsonPath("$.vat").value("123456789"));
    }

    @Test
    void insertTeacher_returns400_whenBeanValidationFails() throws Exception {
        TeacherInsertDTO invalidDTO = TeacherInsertDTO.builder()
                .firstname(null) // @NotNull violated
                .lastname("Doe")
                .vat("123456789")
                .regionId(1L)
                .userInsertDTO(UserInsertDTO.builder().username("jdoe").password("Passw0rd!").roleId(3L).build())
                .personalInfoInsertDTO(PersonalInfoInsertDTO.builder()
                        .amka("12345678901")
                        .identityNumber("AB123456")
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.firstname").exists());

        verify(teacherService, org.mockito.Mockito.never()).saveTeacher(any());
    }

    @Test
    void insertTeacher_returns400_whenCustomValidatorRejectsVat() throws Exception {
        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.rejectValue("vat", "teacher.vat.exists", "Teacher with vat already exists");
            return null;
        }).when(teacherInsertValidator).validate(any(), any());

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInsertDTO())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.vat").value("Teacher with vat already exists"));

        verify(teacherService, org.mockito.Mockito.never()).saveTeacher(any());
    }

    @Test
    void insertTeacher_returns409_whenTeacherAlreadyExists() throws Exception {
        when(teacherService.saveTeacher(any(TeacherInsertDTO.class)))
                .thenThrow(new EntityAlreadyExistsException("Teacher", "Teacher with vat=123456789 already exists"));

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInsertDTO())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TeacherAlreadyExists"))
                .andExpect(jsonPath("$.description").value("Teacher with vat=123456789 already exists"));
    }

    // ---------- POST /api/v1/teachers/{uuid}/amka-file ----------

    @Test
    void uploadAmkaFile_returns204_whenUploadSucceeds() throws Exception {
        UUID uuid = UUID.randomUUID();
        doNothing().when(teacherService).saveAmkaFile(eq(uuid), any());

        MockMultipartFile file = new MockMultipartFile(
                "amkaFile", "amka.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes());

        mockMvc.perform(multipart("/api/v1/teachers/{uuid}/amka-file", uuid).file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    void uploadAmkaFile_returns404_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid))
                .when(teacherService).saveAmkaFile(eq(uuid), any());

        MockMultipartFile file = new MockMultipartFile(
                "amkaFile", "amka.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes());

        mockMvc.perform(multipart("/api/v1/teachers/{uuid}/amka-file", uuid).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    @Test
    void uploadAmkaFile_returns500_whenFileUploadFails() throws Exception {
        UUID uuid = UUID.randomUUID();
        doThrow(new FileUploadException("Error", "Fail to detect file type"))
                .when(teacherService).saveAmkaFile(eq(uuid), any());

        MockMultipartFile file = new MockMultipartFile(
                "amkaFile", "amka.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes());

        mockMvc.perform(multipart("/api/v1/teachers/{uuid}/amka-file", uuid).file(file))
                .andExpect(status().isInternalServerError());
    }

    // ---------- PUT /api/v1/teachers/{uuid} ----------

    @Test
    void updateTeacher_returns200_whenRequestIsValid() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherReadOnlyDTO responseDTO = new TeacherReadOnlyDTO(uuid.toString(), "Johnny", "Doeson", "123456789", "Attica");
        when(teacherService.updateTeacher(any(TeacherUpdateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/teachers/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateDTO(uuid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname").value("Johnny"))
                .andExpect(jsonPath("$.lastname").value("Doeson"));
    }

    @Test
    void updateTeacher_returns404_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.updateTeacher(any(TeacherUpdateDTO.class)))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid + " does not exist"));

        mockMvc.perform(put("/api/v1/teachers/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateDTO(uuid))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- DELETE /api/v1/teachers/{uuid} ----------

    @Test
    void deleteTeacherByUUID_returns200_whenTeacherExists() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherReadOnlyDTO responseDTO = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");
        when(teacherService.deleteTeacherByUUID(uuid)).thenReturn(responseDTO);

        mockMvc.perform(delete("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(uuid.toString()));
    }

    @Test
    void deleteTeacherByUUID_returns404_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.deleteTeacherByUUID(uuid))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid + "not found"));

        mockMvc.perform(delete("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- GET /api/v1/teachers/{uuid} ----------

    @Test
    void getTeacherByUUID_returns200_whenTeacherExists() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherReadOnlyDTO responseDTO = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");
        when(teacherService.getTeacherByUUIDDeletedFalse(uuid)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vat").value("123456789"))
                .andExpect(jsonPath("$.region").value("Attica"));
    }

    @Test
    void getTeacherByUUID_returns404_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.getTeacherByUUIDDeletedFalse(uuid))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid));

        mockMvc.perform(get("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- GET /api/v1/teachers ----------

    @Test
    void getFilteredAndPaginatedTeachers_returns200_withMappedPage() throws Exception {
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(UUID.randomUUID().toString(), "John", "Doe", "123456789", "Attica");
        Page<TeacherReadOnlyDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(teacherService.getTeachersPaginatedFiltered(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers")
                        .param("vat", "123456789")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vat").value("123456789"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFilteredAndPaginatedTeachers_returns404_whenFilterUuidNotFound() throws Exception {
        when(teacherService.getTeachersPaginatedFiltered(any(), any()))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/teachers").param("uuid", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }
}
