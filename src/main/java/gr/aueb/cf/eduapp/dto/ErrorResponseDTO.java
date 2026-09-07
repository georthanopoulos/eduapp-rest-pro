package gr.aueb.cf.eduapp.dto;

public record ErrorResponseDTO(String code, String description) {

    public ErrorResponseDTO(String code) {
        this(code, "");               // the "this" calls the canonical constructor as an alternative, if we want to create "new" without giving a specific description.
    }
}
