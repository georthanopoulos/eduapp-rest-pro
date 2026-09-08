package gr.aueb.cf.eduapp.core.filters;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder                              // @Builder works along with @AllArgsConstructor! Bear in mind that at DTOs where we also have @Builder, an @AllArgsConstructor is NOT needed due to the fact that DTOs are RECORDS!!! In every other case the @Builder feature requires @AllArgsConstructor is MANDATORY!!!
public class TeacherFilters {

    private UUID uuid;
    private String vat;
    private String amka;
    private String lastname;
    private boolean deleted;
    private String region;

}
