package gr.aueb.cf.eduapp.dto;

public record ErrorResponseDTO(String code, String description) {

    public ErrorResponseDTO(String code) {
        this(code, "");               // "this" calls the canonical constructor as an alternative, in case we want to create "new" without giving a specific description.
    }
}
