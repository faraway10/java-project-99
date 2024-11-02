package hexlet.code.dto.label;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabelCreateDTO {
    @Size(min = 1, max = 1000)
    private String name;
}
