package gr.aueb.cf.eduapp.dto;

public record ErrorResponseDTO(String code, String description) {

    public ErrorResponseDTO(String code) {
        this(code, "");               // το This kalei ton canonical constructor,,..san enallaktikh an theloume na kanoyme new xvriw na dvsoyme perigrafh!
    }
}
