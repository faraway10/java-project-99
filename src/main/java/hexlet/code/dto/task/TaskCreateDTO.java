package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateDTO {
    private Integer index;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    @Size(min = 1)
    private String title;

    private String content;

    @NotNull
    private String status;
}
