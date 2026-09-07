package gr.aueb.cf.eduapp.core.filters;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder                              // @Builder works along with @AllArgsConstructor! Bear in mind that at the DTOs where we also have @Builder an @AllArgsConstructor DOES NOT needed due to the fact that DTOs are RECORDS!!! In every other case the @Builder feature alerts that @AllArgsConstructor is MANDATORY!!!
public class TeacherFilters {

    private UUID uuid;
    private String vat;
    private String amka;
    private String lastname;
    private String deleted;
    private String region;

}
