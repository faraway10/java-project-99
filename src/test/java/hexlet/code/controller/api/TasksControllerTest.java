package hexlet.code.controller.api;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import hexlet.code.model.Label;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import hexlet.code.util.ModelGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.mapper.TaskMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import hexlet.code.repository.TaskRepository;
import net.datafaker.Faker;
import org.springframework.web.context.WebApplicationContext;
import hexlet.code.util.Utils;


@SpringBootTest
@AutoConfigureMockMvc
class TasksControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Faker faker;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ModelGenerator modelGenerator;

    private JwtRequestPostProcessor token;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void testIndex() throws Exception {
        var response = mockMvc.perform(get("/api/tasks").with(jwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();

        List<TaskDTO> taskDTOS = om.readValue(body, new TypeReference<>() { });

        var actual = taskDTOS.stream().map(taskMapper::map).toList();
        var expected = taskRepository.findAll();
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testCreate() throws Exception {
        var index = faker.number().positive();
        var assigneeId = userRepository.findByEmail("hexlet@example.com").get().getId();
        var title = String.join(" ", faker.lorem().words(9));
        var content = faker.lorem().paragraph();
        var status = taskStatusRepository.findAll().getFirst().getSlug();
        var taskLabelIds = labelRepository.findAll()
                .stream()
                .map(Label::getId)
                .collect(Collectors.toSet());

        var data = new HashMap<>();
        data.put("index", index);
        data.put("assignee_id", assigneeId);
        data.put("title", title);
        data.put("content", content);
        data.put("status", status);
        data.put("taskLabelIds", taskLabelIds);

        var request = post("/api/tasks").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        var response = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();

        var task = taskRepository.findByName(title).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(task.getIndex()).isEqualTo(index);
        assertThat(task.getAssignee().getId()).isEqualTo(assigneeId);
        assertThat(task.getName()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(content);
        assertThat(task.getTaskStatus().getSlug()).isEqualTo(status);
        assertThat(task.getLabels().stream().map(Label::getId).collect(Collectors.toSet()))
                .isEqualTo(taskLabelIds);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(task.getId()),
                o -> o.node("index").isEqualTo(task.getIndex()),
                o -> o.node("assignee_id").isEqualTo(task.getAssignee().getId()),
                o -> o.node("title").isEqualTo(task.getName()),
                o -> o.node("content").isEqualTo(task.getDescription()),
                o -> o.node("status").isEqualTo(task.getTaskStatus().getSlug()),
                o -> o.node("taskLabelIds")
                        .isEqualTo(task.getLabels().stream().map(Label::getId).collect(Collectors.toSet())),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(task.getCreatedAt()))
        );
    }

    @Test
    public void testShow() throws Exception {
        var task = modelGenerator.getNewSavedTask();

        var request = get("/api/tasks/" + task.getId()).with(jwt());
        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(task.getId()),
                o -> o.node("index").isEqualTo(task.getIndex()),
                o -> o.node("assignee_id").isEqualTo(task.getAssignee().getId()),
                o -> o.node("title").isEqualTo(task.getName()),
                o -> o.node("content").isEqualTo(task.getDescription()),
                o -> o.node("status").isEqualTo(task.getTaskStatus().getSlug()),
                o -> o.node("taskLabelIds")
                        .isEqualTo(task.getLabels().stream().map(Label::getId).collect(Collectors.toSet())),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(task.getCreatedAt()))
        );
    }

    @Test
    public void testUpdate() throws Exception {
        var testTask = modelGenerator.getNewSavedTask();

        var data = new HashMap<>();
        var title = faker.lorem().word();
        var content = faker.lorem().paragraph();
        var status = taskStatusRepository.findAll().getFirst().getSlug();
        var taskLabelIds = Set.of(
                modelGenerator.getNewSavedLabel().getId(),
                modelGenerator.getNewSavedLabel().getId()
        );

        data.put("title", title);
        data.put("content", content);
        data.put("status", status);
        data.put("taskLabelIds", taskLabelIds);

        System.out.println("Debug: " + om.writeValueAsString(data));

        var request = put("/api/tasks/" + testTask.getId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var task = taskRepository.findById(testTask.getId()).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(task.getName()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(content);
        assertThat(task.getTaskStatus().getSlug()).isEqualTo(status);
        assertThat(task.getLabels().stream().map(Label::getId).collect(Collectors.toSet()))
                .isEqualTo(taskLabelIds);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(task.getId()),
                o -> o.node("index").isEqualTo(task.getIndex()),
                o -> o.node("assignee_id").isEqualTo(task.getAssignee().getId()),
                o -> o.node("title").isEqualTo(task.getName()),
                o -> o.node("content").isEqualTo(task.getDescription()),
                o -> o.node("status").isEqualTo(task.getTaskStatus().getSlug()),
                o -> o.node("taskLabelIds")
                        .isEqualTo(task.getLabels().stream().map(Label::getId).collect(Collectors.toSet())),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(task.getCreatedAt()))
        );
    }

    @Test
    public void testDestroy() throws Exception {
        var task = modelGenerator.getNewSavedTask();

        assertTrue(taskRepository.existsById(task.getId()));

        var request = delete("/api/tasks/" + task.getId()).with(jwt());
        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(task.getId()));
    }

    @Test
    public void testCreateNotValid() throws Exception {
        var data = new HashMap<>();
        data.put("title", "");

        var request = post("/api/tasks").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNotValid() throws Exception {
        var testTask = modelGenerator.getNewSavedTask();
        var data = new HashMap<>();
        data.put("title", "");

        var request = put("/api/tasks/" + testTask.getId()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNoAuth() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/tasks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/tasks/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isUnauthorized());
    }
}
