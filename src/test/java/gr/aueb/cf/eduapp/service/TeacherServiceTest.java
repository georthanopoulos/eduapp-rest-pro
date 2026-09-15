package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.core.exceptions.FileUploadException;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.mapper.Mapper;
import gr.aueb.cf.eduapp.model.PersonalInfo;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.PersonalInfoRepository;
import gr.aueb.cf.eduapp.repository.RegionRepository;
import gr.aueb.cf.eduapp.repository.RoleRepository;
import gr.aueb.cf.eduapp.repository.TeacherRepository;
import gr.aueb.cf.eduapp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private Mapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeacherService teacherService;

    private Region region;
    private Role role;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(1L);
        region.setName("Attica");

        role = new Role();
        role.setId(3L);
        role.setName("TEACHER");
    }

    // ---------- helpers ----------

    private TeacherInsertDTO buildInsertDTO() {
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

    private Teacher buildMappedTeacher(TeacherInsertDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setFirstname(dto.firstname());
        teacher.setLastname(dto.lastname());
        teacher.setVat(dto.vat());

        User user = new User();
        user.setUsername(dto.userInsertDTO().username());
        user.setPassword(dto.userInsertDTO().password());
        teacher.addUser(user);

        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setAmka(dto.personalInfoInsertDTO().amka());
        personalInfo.setIdentityNumber(dto.personalInfoInsertDTO().identityNumber());
        personalInfo.setPlaceOfBirth(dto.personalInfoInsertDTO().placeOfBirth());
        personalInfo.setMunicipalityOfRegistration(dto.personalInfoInsertDTO().municipalityOfRegistration());
        teacher.setPersonalInfo(personalInfo);

        return teacher;
    }

    private Teacher buildExistingTeacher() {
        Teacher teacher = new Teacher();
        teacher.setVat("123456789");
        teacher.setFirstname("John");
        teacher.setLastname("Doe");

        User user = new User();
        user.setUsername("jdoe");
        user.setPassword("encoded");
        teacher.addUser(user);

        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setAmka("12345678901");
        personalInfo.setIdentityNumber("AB123456");
        personalInfo.setPlaceOfBirth("Athens");
        personalInfo.setMunicipalityOfRegistration("Athens");
        teacher.setPersonalInfo(personalInfo);

        region.addTeacher(teacher);

        return teacher;
    }

    private TeacherUpdateDTO buildUpdateDTO(UUID uuid, String vat, Long regionId,
                                             String identityNumber, String username) {
        return TeacherUpdateDTO.builder()
                .uuid(uuid)
                .firstname("Johnny")
                .lastname("Doeson")
                .vat(vat)
                .regionId(regionId)
                .userUpdateDTO(UserInsertDTO.builder()
                        .username(username)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("12345678901")
                        .identityNumber(identityNumber)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }

    // ---------- saveTeacher ----------

    @Test
    void saveTeacher_savesSuccessfully_whenDataIsValid() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO();
        Teacher mappedTeacher = buildMappedTeacher(dto);
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(
                UUID.randomUUID().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(mapper.mapToTeacherEntity(dto)).thenReturn(mappedTeacher);
        when(passwordEncoder.encode(dto.userInsertDTO().password())).thenReturn("encodedPassword");
        when(mapper.mapToTeacherReadOnlyDTO(mappedTeacher)).thenReturn(expected);

        TeacherReadOnlyDTO actual = teacherService.saveTeacher(dto);

        assertThat(actual).isEqualTo(expected);
        assertThat(mappedTeacher.getUser().getPassword()).isEqualTo("encodedPassword");
        assertThat(mappedTeacher.getRegion()).isEqualTo(region);
        assertThat(region.getAllTeachers()).contains(mappedTeacher);
        assertThat(role.getAllUsers()).contains(mappedTeacher.getUser());
        verify(teacherRepository).save(mappedTeacher);
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExists_whenVatAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.of(new Teacher()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.saveTeacher(dto));

        verify(personalInfoRepository, never()).findByAmka(anyString());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExists_whenAmkaAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka()))
                .thenReturn(Optional.of(new PersonalInfo()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.saveTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExists_whenIdentityNumberAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.of(new PersonalInfo()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.saveTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExists_whenUsernameAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.of(new User()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.saveTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityInvalidArgument_whenRegionDoesNotExist() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityInvalidArgumentException.class, () -> teacherService.saveTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityInvalidArgument_whenTeacherRoleDoesNotExist() {
        TeacherInsertDTO dto = buildInsertDTO();
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(roleRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(EntityInvalidArgumentException.class, () -> teacherService.saveTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    // ---------- updateTeacher ----------

    @Test
    void updateTeacher_throwsEntityNotFound_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", 1L, "AB123456", "jdoe");
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teacherService.updateTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_updatesSuccessfully_whenNothingUniqueChanges() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", region.getId(), "AB123456", "jdoe");
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(uuid.toString(), "Johnny", "Doeson", "123456789", "Attica");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(expected);

        TeacherReadOnlyDTO actual = teacherService.updateTeacher(dto);

        assertThat(actual).isEqualTo(expected);
        assertThat(existing.getFirstname()).isEqualTo("Johnny");
        assertThat(existing.getLastname()).isEqualTo("Doeson");
        verify(teacherRepository).save(existing);
        verify(regionRepository, never()).findById(any());
        verify(userRepository, never()).findByUsername(anyString());
        verify(personalInfoRepository, never()).findByIdentityNumber(anyString());
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExists_whenNewVatAlreadyUsed() {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "999999999", region.getId(), "AB123456", "jdoe");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(teacherRepository.findByVat("999999999")).thenReturn(Optional.of(new Teacher()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.updateTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExists_whenNewIdentityNumberAlreadyUsed() {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", region.getId(), "CD999999", "jdoe");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(personalInfoRepository.findByIdentityNumber("CD999999")).thenReturn(Optional.of(new PersonalInfo()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.updateTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_throwsEntityInvalidArgument_whenNewRegionDoesNotExist() {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", 2L, "AB123456", "jdoe");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(regionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityInvalidArgumentException.class, () -> teacherService.updateTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_movesTeacherToNewRegion_whenRegionChanges() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        Region newRegion = new Region();
        newRegion.setId(2L);
        newRegion.setName("Thessaly");

        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", 2L, "AB123456", "jdoe");
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(uuid.toString(), "Johnny", "Doeson", "123456789", "Thessaly");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(regionRepository.findById(2L)).thenReturn(Optional.of(newRegion));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(expected);

        TeacherReadOnlyDTO actual = teacherService.updateTeacher(dto);

        assertThat(actual).isEqualTo(expected);
        assertThat(existing.getRegion()).isEqualTo(newRegion);
        assertThat(region.getAllTeachers()).doesNotContain(existing);
        assertThat(newRegion.getAllTeachers()).contains(existing);
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExists_whenNewUsernameAlreadyUsed() {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", region.getId(), "AB123456", "newuser");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(new User()));

        assertThrows(EntityAlreadyExistsException.class, () -> teacherService.updateTeacher(dto));

        verify(teacherRepository, never()).save(any());
    }

    // ---------- deleteTeacherByUUID ----------

    @Test
    void deleteTeacherByUUID_throwsEntityNotFound_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teacherService.deleteTeacherByUUID(uuid));
    }

    @Test
    void deleteTeacherByUUID_softDeletesTeacherPersonalInfoAndUser() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(expected);

        TeacherReadOnlyDTO actual = teacherService.deleteTeacherByUUID(uuid);

        assertThat(actual).isEqualTo(expected);
        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getPersonalInfo().isDeleted()).isTrue();
        assertThat(existing.getUser().isDeleted()).isTrue();
    }

    // ---------- getTeacherByUUID ----------

    @Test
    void getTeacherByUUID_returnsTeacher_whenFound() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(expected);

        assertThat(teacherService.getTeacherByUUID(uuid)).isEqualTo(expected);
    }

    @Test
    void getTeacherByUUID_throwsEntityNotFound_whenMissing() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teacherService.getTeacherByUUID(uuid));
    }

    // ---------- getTeacherByUUIDDeletedFalse ----------

    @Test
    void getTeacherByUUIDDeletedFalse_returnsTeacher_whenFound() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        TeacherReadOnlyDTO expected = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(expected);

        assertThat(teacherService.getTeacherByUUIDDeletedFalse(uuid)).isEqualTo(expected);
    }

    @Test
    void getTeacherByUUIDDeletedFalse_throwsEntityNotFound_whenMissing() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teacherService.getTeacherByUUIDDeletedFalse(uuid));
    }

    // ---------- pagination ----------

    @Test
    void getPaginatedTeachers_mapsPageOfTeachersToPageOfDTOs() {
        Teacher existing = buildExistingTeacher();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(existing.getUuid().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findAll(pageable)).thenReturn(teacherPage);
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getPaginatedTeachers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void getPaginatedTeachersDeletedFalse_mapsPageOfTeachersToPageOfDTOs() {
        Teacher existing = buildExistingTeacher();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(existing.getUuid().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findAllByDeletedFalse(pageable)).thenReturn(teacherPage);
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getPaginatedTeachersDeletedFalse(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(dto);
    }

    // ---------- getTeachersPaginatedFiltered ----------

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResult_whenFilteringByUuid() throws Exception {
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().uuid(uuid).build();
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(uuid.toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(dto);
        verify(teacherRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void getTeachersPaginatedFiltered_throwsEntityNotFound_whenUuidFilterDoesNotMatch() {
        UUID uuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().uuid(uuid).build();

        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> teacherService.getTeachersPaginatedFiltered(pageable, filters));
    }

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResult_whenFilteringByAmka() throws Exception {
        Teacher existing = buildExistingTeacher();
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().amka("12345678901").build();
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(existing.getUuid().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByPersonalInfo_Amka("12345678901")).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResult_whenFilteringByVat() throws Exception {
        Teacher existing = buildExistingTeacher();
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().vat("123456789").build();
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(existing.getUuid().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findByVatAndDeletedFalse("123456789")).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void getTeachersPaginatedFiltered_delegatesToSpecification_whenNoDirectFilterGiven() throws Exception {
        Teacher existing = buildExistingTeacher();
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().lastname("Doe").build();
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);
        TeacherReadOnlyDTO dto = new TeacherReadOnlyDTO(existing.getUuid().toString(), "John", "Doe", "123456789", "Attica");

        when(teacherRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(teacherPage);
        when(mapper.mapToTeacherReadOnlyDTO(existing)).thenReturn(dto);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(dto);
        verify(teacherRepository, never()).findByUuidAndDeletedFalse(any());
        verify(teacherRepository, never()).findByPersonalInfo_Amka(anyString());
        verify(teacherRepository, never()).findByVatAndDeletedFalse(anyString());
    }

    // ---------- isTeacherExistsByVat ----------

    @Test
    void isTeacherExistsByVat_returnsTrue_whenTeacherExists() {
        when(teacherRepository.findByVat("123456789")).thenReturn(Optional.of(new Teacher()));
        assertThat(teacherService.isTeacherExistsByVat("123456789")).isTrue();
    }

    @Test
    void isTeacherExistsByVat_returnsFalse_whenTeacherDoesNotExist() {
        when(teacherRepository.findByVat("000000000")).thenReturn(Optional.empty());
        assertThat(teacherService.isTeacherExistsByVat("000000000")).isFalse();
    }

    // ---------- saveAmkaFile ----------

    @Test
    void saveAmkaFile_throwsEntityNotFound_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teacherService.saveAmkaFile(uuid, file));
    }

    @Test
    void saveAmkaFile_throwsFileUploadException_whenContentTypeCannotBeDetected() throws Exception {
        ReflectionTestUtils.setField(teacherService, "uploadDir", "uploads-test");
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThrows(FileUploadException.class, () -> teacherService.saveAmkaFile(uuid, file));
        assertThat(existing.getPersonalInfo().getAmkaFile()).isNull();
    }

    @Test
    void saveAmkaFile_attachesFile_whenTeacherExists() throws Exception {
        ReflectionTestUtils.setField(teacherService, "uploadDir", "uploads-test");
        Teacher existing = buildExistingTeacher();
        UUID uuid = existing.getUuid();
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        byte[] content = "hello world".getBytes();

        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));

        TransactionSynchronizationManager.initSynchronization();
        try {
            teacherService.saveAmkaFile(uuid, file);

            assertThat(existing.getPersonalInfo().getAmkaFile()).isNotNull();
            assertThat(existing.getPersonalInfo().getAmkaFile().getFilename()).isEqualTo("doc.pdf");
            assertThat(existing.getPersonalInfo().getAmkaFile().getExtension()).isEqualTo(".pdf");
            assertThat(existing.getPersonalInfo().getAmkaFile().getSavedName()).endsWith(".pdf");
            assertThat(existing.getPersonalInfo().getAmkaFile().getContentType()).isNotBlank();
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
