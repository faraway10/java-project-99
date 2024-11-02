package hexlet.code.mapper;

import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.model.Label;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskStatusRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.model.Task;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class TaskMapper {
    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Mapping(source = "title", target = "name")
    @Mapping(source = "content", target = "description")
    @Mapping(source = "assigneeId", target = "assignee")
    @Mapping(source = "status", target = "taskStatus", qualifiedByName = "mapStatus")
    @Mapping(source = "taskLabelIds", target = "labels", qualifiedByName = "labelIdsToLabels")
    public abstract Task map(TaskCreateDTO model);

    @Mapping(source = "name", target = "title")
    @Mapping(source = "description", target = "content")
    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "taskStatus.slug", target = "status")
    @Mapping(source = "labels", target = "taskLabelIds", qualifiedByName = "labelsToLabelIds")
    public abstract TaskDTO map(Task model);

    @Mapping(source = "title", target = "name")
    @Mapping(source = "content", target = "description")
    @Mapping(source = "assigneeId", target = "assignee.id")
    @Mapping(source = "status", target = "taskStatus", qualifiedByName = "mapStatus")
    @Mapping(source = "taskLabelIds", target = "labels", qualifiedByName = "labelIdsToLabels")
    public abstract Task map(TaskDTO dto);

    @Mapping(source = "title", target = "name")
    @Mapping(source = "content", target = "description")
    @Mapping(source = "assigneeId", target = "assignee")
    @Mapping(source = "status", target = "taskStatus", qualifiedByName = "mapStatus")
    @Mapping(source = "taskLabelIds", target = "labels", qualifiedByName = "labelIdsToLabels")
    public abstract void update(TaskUpdateDTO dto, @MappingTarget Task model);

    @Named("mapStatus")
    final TaskStatus mapStatus(String slug) {
        return taskStatusRepository.findBySlug(slug).orElseThrow(
                () -> new ResourceNotFoundException("Task status with slug " + slug + " not found")
        );
    }

    @Named("labelIdsToLabels")
    final Set<Label> labelIdsToLabels(Set<Long> labelIds) {
        return labelIds == null ? new HashSet<>()
                : labelRepository.findByIdIn(labelIds);
    }

    @Named("labelsToLabelIds")
    final Set<Long> labelsToLabelIds(Set<Label> labels) {
        return labels == null ? new HashSet<>()
                : labels.stream()
                .map(Label::getId)
                .collect(Collectors.toSet());
    }
}
