package gr.aueb.cf.eduapp.core.exceptions;

import lombok.Getter;
import org.springframework.validation.BindingResult;

@Getter
public class ValidationException extends AppGenericException {

    private static final String DEFAULT_CODE = "ValidationError";        // it must be "static" because as soon as the constructor is called the super feature must be executed first! Static in order to be used at the concat of the super.
    private final BindingResult bindingResult;

    public ValidationException(String code, String message, BindingResult bindingResult) {
        super(code + DEFAULT_CODE, message);
        this.bindingResult = bindingResult;
    }
}
