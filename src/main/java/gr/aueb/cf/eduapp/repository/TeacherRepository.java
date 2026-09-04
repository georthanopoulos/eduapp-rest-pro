package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {

    Optional<Teacher> findByUuid(UUID uuid);
    Optional<Teacher> findByUuidAndDeletedFalse(UUID uuid);

    Optional<Teacher> findByVat(String vat);
    Optional<Teacher> findByVatAndDeletedFalse(String vat);

    Optional<Teacher> findByPersonalInfo_Amka(String amka);     // amka is field of the personal info (implicit join). Noted with underscore for our understanding only!

    @EntityGraph(attributePaths = {"personalInfo", "region"})
    Page<Teacher> findAllByDeletedFalse(Pageable pageable);

    boolean existsByUuidAndUser_Uuid(UUID teacherUuid, UUID userUuid);      // Cross-checking between user and teacher uuid relationship! The two uuid's are not the same! they are just connected in a certain way!.
}
