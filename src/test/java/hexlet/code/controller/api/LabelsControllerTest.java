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

import hexlet.code.repository.TaskRepository;
import hexlet.code.util.ModelGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import hexlet.code.dto.label.LabelDTO;
import hexlet.code.mapper.LabelMapper;
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
import hexlet.code.repository.LabelRepository;
import net.datafaker.Faker;
import org.springframework.web.context.WebApplicationContext;
import hexlet.code.util.Utils;


@SpringBootTest
@AutoConfigureMockMvc
class LabelsControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Faker faker;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LabelMapper labelMapper;

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
        var response = mockMvc.perform(get("/api/labels").with(jwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();

        List<LabelDTO> labelDTOS = om.readValue(body, new TypeReference<>() { });

        var actual = labelDTOS.stream().map(labelMapper::map).toList();
        var expected = labelRepository.findAll();
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testCreate() throws Exception {
        var name = String.join(" ", faker.lorem().words(3));

        var data = new HashMap<>();
        data.put("name", name);

        var request = post("/api/labels").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        var response = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();

        var label = labelRepository.findByName(name).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(label.getName()).isEqualTo(name);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(label.getId()),
                o -> o.node("name").isEqualTo(label.getName()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(label.getCreatedAt()))
        );
    }

    @Test
    public void testShow() throws Exception {
        var label = modelGenerator.getNewSavedLabel();
        var request = get("/api/labels/" + label.getId()).with(jwt());
        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(label.getId()),
                o -> o.node("name").isEqualTo(label.getName()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(label.getCreatedAt()))
        );
    }

    @Test
    public void testUpdate() throws Exception {
        var testLabel = modelGenerator.getNewSavedLabel();
        var data = new HashMap<>();
        var name = String.join(" ", faker.lorem().words(3));
        data.put("name", name);

        var request = put("/api/labels/" + testLabel.getId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var label = labelRepository.findById(testLabel.getId()).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(label.getName()).isEqualTo(name);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(label.getId()),
                o -> o.node("name").isEqualTo(label.getName()),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(label.getCreatedAt()))
        );
    }

    @Test
    public void testDestroy() throws Exception {
        var label = modelGenerator.getNewSavedLabel();

        assertTrue(labelRepository.existsById(label.getId()));

        var request = delete("/api/labels/" + label.getId()).with(jwt());
        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertFalse(labelRepository.existsById(label.getId()));
    }

    @Test
    public void testDestroyIfLabelInUse() throws Exception {
        var label = modelGenerator.getNewSavedLabel();
        var task = modelGenerator.getNewSavedTask();
        task.setLabels(Set.of(label));
        taskRepository.save(task);

        var request = delete("/api/labels/" + label.getId()).with(jwt());
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        assertTrue(labelRepository.existsById(label.getId()));
    }

    @Test
    public void testCreateNotValid() throws Exception {
        var data = new HashMap<>();
        data.put("name", "");

        var request = post("/api/labels").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNotValid() throws Exception {
        var testLabel = modelGenerator.getNewSavedLabel();
        var data = new HashMap<>();
        data.put("name", "");

        var request = put("/api/labels/" + testLabel.getId()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNoAuth() throws Exception {
        mockMvc.perform(get("/api/labels"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/labels"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/labels/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/labels/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/labels/1"))
                .andExpect(status().isUnauthorized());
    }
}
