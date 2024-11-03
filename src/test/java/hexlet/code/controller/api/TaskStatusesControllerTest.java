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

import hexlet.code.util.ModelGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import hexlet.code.dto.taskstatus.TaskStatusDTO;
import hexlet.code.mapper.TaskStatusMapper;
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
import hexlet.code.repository.TaskStatusRepository;
import net.datafaker.Faker;
import org.springframework.web.context.WebApplicationContext;
import hexlet.code.util.Utils;


@SpringBootTest
@AutoConfigureMockMvc
class TaskStatusesControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Faker faker;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TaskStatusMapper taskStatusMapper;

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
        var response = mockMvc.perform(get("/api/task_statuses").with(jwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();

        List<TaskStatusDTO> taskStatusDTOS = om.readValue(body, new TypeReference<>() { });

        var actual = taskStatusDTOS.stream().map(taskStatusMapper::map).toList();
        var expected = taskStatusRepository.findAll();
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testCreate() throws Exception {
        var name = faker.lorem().word();
        var slug = String.join("_", faker.lorem().words(9));

        var data = new HashMap<>();
        data.put("name", name);
        data.put("slug", slug);

        var request = post("/api/task_statuses").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        var response = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();

        var taskStatus = taskStatusRepository.findBySlug(slug).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(taskStatus.getName()).isEqualTo(name);
        assertThat(taskStatus.getSlug()).isEqualTo(slug);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(taskStatus.getId()),
                o -> o.node("name").isEqualTo(taskStatus.getName()),
                o -> o.node("slug").isEqualTo(taskStatus.getSlug()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(taskStatus.getCreatedAt()))
        );
    }

    @Test
    public void testShow() throws Exception {
        var taskStatus = modelGenerator.getNewSavedTaskStatus();
        var request = get("/api/task_statuses/" + taskStatus.getId()).with(jwt());
        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(taskStatus.getId()),
                o -> o.node("name").isEqualTo(taskStatus.getName()),
                o -> o.node("slug").isEqualTo(taskStatus.getSlug()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(taskStatus.getCreatedAt()))
        );
    }

    @Test
    public void testUpdate() throws Exception {
        var testTaskStatus = modelGenerator.getNewSavedTaskStatus();
        var data = new HashMap<>();
        var name = faker.lorem().word();
        data.put("name", name);

        var request = put("/api/task_statuses/" + testTaskStatus.getId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var taskStatus = taskStatusRepository.findById(testTaskStatus.getId()).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(taskStatus.getName()).isEqualTo(name);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(taskStatus.getId()),
                o -> o.node("name").isEqualTo(taskStatus.getName()),
                o -> o.node("slug").isEqualTo(taskStatus.getSlug()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(taskStatus.getCreatedAt()))
        );
    }

    @Test
    public void testDestroy() throws Exception {
        var taskStatus = modelGenerator.getNewSavedTaskStatus();

        assertTrue(taskStatusRepository.existsById(taskStatus.getId()));

        var request = delete("/api/task_statuses/" + taskStatus.getId()).with(jwt());
        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertFalse(taskStatusRepository.existsById(taskStatus.getId()));
    }

    @Test
    public void testDestroyIfStatusInUse() throws Exception {
        var task = modelGenerator.getNewSavedTask();
        var taskStatus = task.getTaskStatus();

        var request = delete("/api/task_statuses/" + taskStatus.getId()).with(jwt());
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        assertTrue(taskStatusRepository.existsById(taskStatus.getId()));
    }

    @Test
    public void testCreateNotValid() throws Exception {
        var data = new HashMap<>();
        data.put("slug", "");

        var request = post("/api/task_statuses").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNotValid() throws Exception {
        var testTaskStatus = modelGenerator.getNewSavedTaskStatus();
        var data = new HashMap<>();
        data.put("slug", "");

        var request = put("/api/task_statuses/" + testTaskStatus.getId()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNoAuth() throws Exception {
        mockMvc.perform(get("/api/task_statuses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/task_statuses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/task_statuses/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/task_statuses/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/task_statuses/1"))
                .andExpect(status().isUnauthorized());
    }
}
