package gr.aueb.cf.eduapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TeacherUpdateDTO(

        @NotNull
        UUID uuid,

        @NotNull
        @Size(min = 2)
        String firstname,

        @NotNull
        @Size(min = 2)
        String lastname,

        @Pattern(regexp = "\\d{9,}")
        String vat,

        @NotNull
        Long regionId,

        @NotNull
        @Valid
        UserUpdateDTO userUpdateDTO,         // Corrected the in class UserInsertDTO userUpdateDTO (probably mistaken code!)

        @NotNull
        @Valid
        PersonalInfoUpdateDTO personalInfoUpdateDTO   // Corrected the in class PersonalInfoDTO personalUpdateDTO (probably mistaken code!)
) {}
