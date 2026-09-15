package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.repository.RegionRepository;
import gr.aueb.cf.eduapp.repository.TeacherRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Full-context integration tests for {@link TeacherService}, backed by a real
 * MySQL instance via Testcontainers. Flyway migrations (schema + seed data for
 * regions/roles/capabilities) run automatically against the container on
 * context startup, so tests rely on that seeded reference data.
 * <p>
 * Each test runs inside its own Spring-managed transaction that is rolled
 * back afterwards, keeping the container's data clean between tests without
 * needing manual cleanup.
 */
@SpringBootTest
@Testcontainers
@Transactional
@WithMockUser(username = "admin", authorities = {"EDIT_TEACHER", "DELETE_TEACHER", "VIEW_TEACHER", "VIEW_TEACHERS"})
class TeacherServiceIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36");

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    // ---------- helpers ----------

    private TeacherInsertDTO buildInsertDTO(String unique, Long regionId) {
        return TeacherInsertDTO.builder()
                .firstname("Jane")
                .lastname("Doe")
                .vat("vat-" + unique)
                .regionId(regionId)
                .userInsertDTO(UserInsertDTO.builder()
                        .username("user-" + unique)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoInsertDTO(PersonalInfoInsertDTO.builder()
                        .amka("amka-" + unique)
                        .identityNumber("id-" + unique)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }

    private Long firstSeededRegionId() {
        return regionRepository.findAllByOrderByNameAsc().get(0).getId();
    }

    // ---------- saveTeacher ----------

    @Test
    void saveTeacher_persistsTeacherUserAndPersonalInfo_endToEnd() throws Exception {
        Long regionId = firstSeededRegionId();
        String unique = UUID.randomUUID().toString();
        TeacherInsertDTO dto = buildInsertDTO(unique, regionId);

        TeacherReadOnlyDTO result = teacherService.saveTeacher(dto);

        entityManager.flush();
        entityManager.clear();

        assertThat(result.vat()).isEqualTo("vat-" + unique);
        assertThat(result.firstname()).isEqualTo("Jane");

        Teacher persisted = teacherRepository.findByVat("vat-" + unique).orElseThrow();
        assertThat(persisted.getUuid().toString()).isEqualTo(result.uuid());
        assertThat(persisted.getRegion().getId()).isEqualTo(regionId);
        assertThat(persisted.getPersonalInfo().getAmka()).isEqualTo("amka-" + unique);
        assertThat(persisted.getUser().getUsername()).isEqualTo("user-" + unique);

        // password must have been BCrypt-encoded by the service, never stored raw
        assertThat(persisted.getUser().getPassword()).isNotEqualTo("Passw0rd!");
        assertThat(passwordEncoder.matches("Passw0rd!", persisted.getUser().getPassword())).isTrue();
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExists_whenVatIsDuplicated() throws Exception {
        Long regionId = firstSeededRegionId();
        String unique = UUID.randomUUID().toString();
        teacherService.saveTeacher(buildInsertDTO(unique, regionId));

        String otherUnique = UUID.randomUUID().toString();
        TeacherInsertDTO duplicateVatDto = TeacherInsertDTO.builder()
                .firstname("Other")
                .lastname("Person")
                .vat("vat-" + unique) // duplicate
                .regionId(regionId)
                .userInsertDTO(UserInsertDTO.builder()
                        .username("user-" + otherUnique)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoInsertDTO(PersonalInfoInsertDTO.builder()
                        .amka("amka-" + otherUnique)
                        .identityNumber("id-" + otherUnique)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();

        assertThrows(EntityAlreadyExistsException.class,
                () -> teacherService.saveTeacher(duplicateVatDto));
    }

    // ---------- updateTeacher ----------

    @Test
    void updateTeacher_persistsChanges_includingRegionMove() throws Exception {
        List<Region> regions = regionRepository.findAllByOrderByNameAsc();
        Region originalRegion = regions.get(0);
        Region newRegion = regions.get(1);

        String unique = UUID.randomUUID().toString();
        TeacherReadOnlyDTO created = teacherService.saveTeacher(buildInsertDTO(unique, originalRegion.getId()));
        UUID uuid = UUID.fromString(created.uuid());

        TeacherUpdateDTO updateDTO = TeacherUpdateDTO.builder()
                .uuid(uuid)
                .firstname("Janet")
                .lastname("Doeson")
                .vat("vat-" + unique)
                .regionId(newRegion.getId())
                .userUpdateDTO(UserInsertDTO.builder()
                        .username("user-" + unique)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("amka-" + unique)
                        .identityNumber("id-" + unique)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();

        TeacherReadOnlyDTO result = teacherService.updateTeacher(updateDTO);

        entityManager.flush();
        entityManager.clear();

        assertThat(result.firstname()).isEqualTo("Janet");
        assertThat(result.region()).isEqualTo(newRegion.getName());

        Teacher persisted = teacherRepository.findByUuid(uuid).orElseThrow();
        assertThat(persisted.getFirstname()).isEqualTo("Janet");
        assertThat(persisted.getLastname()).isEqualTo("Doeson");
        assertThat(persisted.getRegion().getId()).isEqualTo(newRegion.getId());
    }

    @Test
    void updateTeacher_throwsEntityNotFound_whenUuidUnknown() {
        TeacherUpdateDTO updateDTO = TeacherUpdateDTO.builder()
                .uuid(UUID.randomUUID())
                .firstname("Ghost")
                .lastname("Teacher")
                .vat("vat-ghost")
                .regionId(firstSeededRegionId())
                .userUpdateDTO(UserInsertDTO.builder()
                        .username("ghost-user")
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("00000000000")
                        .identityNumber("ghost-id")
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();

        assertThrows(EntityNotFoundException.class, () -> teacherService.updateTeacher(updateDTO));
    }

    // ---------- deleteTeacherByUUID ----------

    @Test
    void deleteTeacherByUUID_softDeletesTeacherUserAndPersonalInfo() throws Exception {
        Long regionId = firstSeededRegionId();
        String unique = UUID.randomUUID().toString();
        TeacherReadOnlyDTO created = teacherService.saveTeacher(buildInsertDTO(unique, regionId));
        UUID uuid = UUID.fromString(created.uuid());

        teacherService.deleteTeacherByUUID(uuid);

        entityManager.flush();
        entityManager.clear();

        assertThat(teacherRepository.findByUuidAndDeletedFalse(uuid)).isEmpty();

        Teacher persisted = teacherRepository.findByUuid(uuid).orElseThrow();
        assertThat(persisted.isDeleted()).isTrue();
        assertThat(persisted.getPersonalInfo().isDeleted()).isTrue();
        assertThat(persisted.getUser().isDeleted()).isTrue();
    }

    // ---------- pagination ----------

    @Test
    void getPaginatedTeachersDeletedFalse_excludesSoftDeletedTeachers() throws Exception {
        Long regionId = firstSeededRegionId();
        String activeUnique = UUID.randomUUID().toString();
        String deletedUnique = UUID.randomUUID().toString();

        TeacherReadOnlyDTO active = teacherService.saveTeacher(buildInsertDTO(activeUnique, regionId));
        TeacherReadOnlyDTO toDelete = teacherService.saveTeacher(buildInsertDTO(deletedUnique, regionId));
        teacherService.deleteTeacherByUUID(UUID.fromString(toDelete.uuid()));

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 50);
        Page<TeacherReadOnlyDTO> page = teacherService.getPaginatedTeachersDeletedFalse(pageable);

        assertThat(page.getContent())
                .extracting(TeacherReadOnlyDTO::uuid)
                .contains(active.uuid())
                .doesNotContain(toDelete.uuid());
    }
}
