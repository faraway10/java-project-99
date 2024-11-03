package hexlet.code.util;

import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public final class ModelGenerator {
    @Autowired
    private Faker faker;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private LabelRepository labelRepository;

    public User getNewSavedUser() {
        var user = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .supply(Select.field(User::getFirstName), () -> faker.name().firstName())
                .supply(Select.field(User::getLastName), () -> faker.name().lastName())
                .supply(Select.field(User::getEmail), () -> faker.internet().emailAddress())
                .supply(Select.field(User::getPassword), () -> faker.internet().password(3, 32))
                .create();
        userRepository.save(user);
        return user;
    }

    public TaskStatus getNewSavedTaskStatus() {
        var taskStatus = Instancio.of(TaskStatus.class)
                .ignore(Select.field(TaskStatus::getId))
                .supply(Select.field(TaskStatus::getName), () -> faker.lorem().word())
                .supply(Select.field(TaskStatus::getSlug), () -> String.join("_", faker.lorem().words(9)))
                .create();
        taskStatusRepository.save(taskStatus);
        return taskStatus;
    }

    public Task getNewSavedTask() {
        var task = Instancio.of(Task.class)
                .ignore(Select.field(Task::getId))
                .supply(Select.field(Task::getName), () -> String.join(" ", faker.lorem().words(9)))
                .supply(Select.field(Task::getIndex), () -> faker.number().positive())
                .supply(Select.field(Task::getDescription), () -> faker.lorem().paragraph())
                .supply(Select.field(Task::getTaskStatus), () -> taskStatusRepository.findAll().getFirst())
                .supply(Select.field(Task::getAssignee), () -> userRepository.findAll().getFirst())
                .supply(Select.field(Task::getLabels), () -> Set.of(
                        getNewSavedLabel(),
                        getNewSavedLabel()
                ))
                .create();
        taskRepository.save(task);
        return task;
    }

    public Label getNewSavedLabel() {
        var label = Instancio.of(Label.class)
                .ignore(Select.field(Label::getId))
                .supply(Select.field(Label::getName), () -> String.join(" ", faker.lorem().words(9)))
                .create();
        labelRepository.save(label);
        return label;
    }
}
