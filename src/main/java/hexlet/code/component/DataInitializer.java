package hexlet.code.component;

import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.mapper.UserMapper;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;

import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.List;

@Component
@AllArgsConstructor
class DataInitializer implements ApplicationRunner {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final UserMapper userMapper;

    @Autowired
    private final TaskStatusRepository taskStatusRepository;

    @Autowired
    private final TaskRepository taskRepository;

    @Autowired
    private final LabelRepository labelRepository;

    private final List<String> defaultTaskStatusSlugs = List.of(
            "draft",
            "to_review",
            "to_be_fixed",
            "to_publish",
            "published"
    );

    private final List<String> defaultLabels = List.of(
            "feature",
            "bug"
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var userData = new UserCreateDTO();
        userData.setEmail("hexlet@example.com");
        userData.setFirstName("tirion");
        userData.setLastName("lannister");
        userData.setPassword("qwerty");
        var user = userMapper.map(userData);
        userRepository.save(user);

        for (var slug : defaultTaskStatusSlugs) {
            var taskStatus = new TaskStatus();
            var name = WordUtils.capitalizeFully(slug.replace('_', ' '));
            taskStatus.setName(name);
            taskStatus.setSlug(slug);
            taskStatusRepository.save(taskStatus);
        }

        for (var labelName : defaultLabels) {
            var label = new Label();
            label.setName(labelName);
            labelRepository.save(label);
        }

        var task = new Task();
        task.setIndex(42);
        task.setAssignee(user);
        task.setName("Init task");
        task.setTaskStatus(taskStatusRepository.findAll().getFirst());
        task.setLabels(new HashSet<>(labelRepository.findAll()));
        taskRepository.save(task);
    }
}
