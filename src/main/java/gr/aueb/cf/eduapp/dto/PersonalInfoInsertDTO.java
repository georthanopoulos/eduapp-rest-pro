package gr.aueb.cf.eduapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder                                        // Instead of bigger constructors when we have many fields. A better pattern through lombok. (Not @AllArgsConstructor is needed as we are in a record and not in a class).
public record PersonalInfoInsertDTO(

        @NotNull
        @Pattern(regexp = "\\d{11}")
        String amka,

        @NotBlank                                         // neither null nor empty
        String identityNumber,

        @NotBlank
        String placeOfBirth,

        @NotBlank
        String municipalityOfRegistration
) {}
