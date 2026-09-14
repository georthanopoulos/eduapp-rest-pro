package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.PersonalInfo;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Repository slice test backed by a real MySQL instance via Testcontainers.
 * Flyway migrations (schema + seed data for regions/roles/capabilities) run
 * automatically against the container on context startup, so tests rely on
 * that seeded reference data instead of re-creating it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class TeacherRepositoryTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36");

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Region region;
    private Role teacherRole;

    @BeforeEach
    void setUp() {
        region = regionRepository.findAllByOrderByNameAsc().get(0);
        teacherRole = entityManager.getEntityManager()
                .createQuery("select r from Role r where r.name = :name", Role.class)
                .setParameter("name", "TEACHER")
                .getSingleResult();
    }

    @Test
    void findByUuid_whenTeacherExists_returnsTeacher() {
        Teacher teacher = persistTeacher();

        Optional<Teacher> found = teacherRepository.findByUuid(teacher.getUuid());

        assertThat(found).isPresent();
        assertThat(found.get().getVat()).isEqualTo(teacher.getVat());
    }

    @Test
    void findByUuid_whenTeacherDoesNotExist_returnsEmpty() {
        assertThat(teacherRepository.findByUuid(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByUuidAndDeletedFalse_excludesSoftDeletedTeacher() {
        Teacher teacher = persistTeacher();
        teacher.softDelete();
        entityManager.flush();

        assertThat(teacherRepository.findByUuidAndDeletedFalse(teacher.getUuid())).isEmpty();
        assertThat(teacherRepository.findByUuid(teacher.getUuid())).isPresent();
    }

    @Test
    void findByVat_andFindByVatAndDeletedFalse_behaveAsExpected() {
        Teacher teacher = persistTeacher();

        assertThat(teacherRepository.findByVat(teacher.getVat())).isPresent();
        assertThat(teacherRepository.findByVatAndDeletedFalse(teacher.getVat())).isPresent();

        teacher.softDelete();
        entityManager.flush();

        assertThat(teacherRepository.findByVatAndDeletedFalse(teacher.getVat())).isEmpty();
        assertThat(teacherRepository.findByVat(teacher.getVat())).isPresent();
    }

    @Test
    void findByPersonalInfoAmka_whenAmkaMatches_returnsTeacher() {
        Teacher teacher = persistTeacher();

        Optional<Teacher> found = teacherRepository.findByPersonalInfo_Amka(teacher.getPersonalInfo().getAmka());

        assertThat(found).isPresent();
        assertThat(found.get().getUuid()).isEqualTo(teacher.getUuid());
    }

    @Test
    void findByPersonalInfoAmka_whenAmkaDoesNotMatch_returnsEmpty() {
        persistTeacher();

        assertThat(teacherRepository.findByPersonalInfo_Amka("does-not-exist")).isEmpty();
    }

    @Test
    void findAllByDeletedFalse_returnsOnlyNonDeletedTeachers() {
        Teacher active = persistTeacher();
        Teacher deleted = persistTeacher();
        deleted.softDelete();
        entityManager.flush();

        Page<Teacher> page = teacherRepository.findAllByDeletedFalse(PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Teacher::getUuid)
                .contains(active.getUuid())
                .doesNotContain(deleted.getUuid());
    }

    @Test
    void existsByUuidAndUserUuid_trueForMatchingPair_falseForMismatch() {
        Teacher teacher = persistTeacher();
        Teacher other = persistTeacher();

        assertThat(teacherRepository.existsByUuidAndUser_Uuid(teacher.getUuid(), teacher.getUser().getUuid()))
                .isTrue();
        assertThat(teacherRepository.existsByUuidAndUser_Uuid(teacher.getUuid(), other.getUser().getUuid()))
                .isFalse();
    }

    private Teacher persistTeacher() {
        String unique = UUID.randomUUID().toString();

        User user = new User("user-" + unique, "encoded-password");
        teacherRole.addUser(user);

        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setAmka("amka-" + unique);
        personalInfo.setIdentityNumber("id-" + unique);
        personalInfo.setPlaceOfBirth("Athens");
        personalInfo.setMunicipalityOfRegistration("Athens");

        Teacher teacher = new Teacher();
        teacher.setFirstname("Jane");
        teacher.setLastname("Doe");
        teacher.setVat("vat-" + unique);
        teacher.addUser(user);
        teacher.setPersonalInfo(personalInfo);
        region.addTeacher(teacher);

        return teacherRepository.saveAndFlush(teacher);
    }
}
