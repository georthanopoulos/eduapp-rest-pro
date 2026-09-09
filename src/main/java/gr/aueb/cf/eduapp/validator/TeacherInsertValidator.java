package gr.aueb.cf.eduapp.validator;

import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.service.ITeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherInsertValidator implements Validator {

    private final ITeacherService teacherService;

    @Override
    public boolean supports(Class<?> clazz) {                          // represents a description of the class itself!
        return TeacherInsertDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TeacherInsertDTO teacherInsertDTO = (TeacherInsertDTO) target;

        if (teacherInsertDTO.vat() != null
                && teacherService.isTeacherExistsByVat(teacherInsertDTO.vat())) {
            log.warn("Validation failed. Teacher with vat= {} already exists", teacherInsertDTO.vat());
            errors.rejectValue("vat", "teacher.vat.exists", "Teacher with vat=" + teacherInsertDTO + "already exists");
        }
    }

    // TODO: more validations amka, identity number, username, etc unique fields.

}
