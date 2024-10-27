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

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import hexlet.code.dto.UserDTO;
import hexlet.code.mapper.UserMapper;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.web.context.WebApplicationContext;
import hexlet.code.util.Utils;


@SpringBootTest
@AutoConfigureMockMvc
class UsersControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Faker faker;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder encoder;

    private JwtRequestPostProcessor token;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity())
                .build();

        token = jwt().jwt(builder -> builder.subject("hexlet@example.com"));
    }

    private User getNewSavedUser() {
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

    @Test
    public void testIndex() throws Exception {
        var response = mockMvc.perform(get("/api/users").with(jwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var body = response.getContentAsString();

        List<UserDTO> userDTOS = om.readValue(body, new TypeReference<>() { });

        var actual = userDTOS.stream().map(userMapper::map).toList();
        var expected = userRepository.findAll();
        Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testCreate() throws Exception {
        var firstName = faker.name().firstName();
        var lastName = faker.name().lastName();
        var email = faker.internet().emailAddress();
        var password = faker.internet().password(3, 32);

        var data = new HashMap<>();
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("email", email);
        data.put("password", password);

        var request = post("/api/users").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        var response = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();

        var user = userRepository.findByEmail(email).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getEmail()).isEqualTo(email);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        System.out.println("Debug: " + responseBody);
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(user.getId()),
                o -> o.node("firstName").isEqualTo(user.getFirstName()),
                o -> o.node("lastName").isEqualTo(user.getLastName()),
                o -> o.node("email").isEqualTo(user.getEmail()),
                o -> o.node("password").isAbsent(),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(user.getCreatedAt()))
        );
    }

    @Test
    public void testShow() throws Exception {
        var user = getNewSavedUser();
        var request = get("/api/users/" + user.getId()).with(jwt());
        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(user.getId()),
                o -> o.node("firstName").isEqualTo(user.getFirstName()),
                o -> o.node("lastName").isEqualTo(user.getLastName()),
                o -> o.node("email").isEqualTo(user.getEmail()),
                o -> o.node("password").isAbsent(),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(user.getCreatedAt()))
        );
    }

    @Test
    public void testSelfUpdate() throws Exception {
        var testUser = userRepository.findByEmail("hexlet@example.com").get();
        var data = new HashMap<>();
        var firstName = faker.name().firstName();
        data.put("firstName", firstName);

        var request = put("/api/users/" + testUser.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var user = userRepository.findById(testUser.getId()).get();

        // Сравниваем данные модели с данными в запросе к методу
        assertThat(user.getFirstName()).isEqualTo(firstName);

        // Сравниваем данные модели с данными в ответе метода
        var responseBody = response.getResponse().getContentAsString();
        assertThatJson(responseBody).and(
                o -> o.node("id").isEqualTo(user.getId()),
                o -> o.node("firstName").isEqualTo(user.getFirstName()),
                o -> o.node("lastName").isEqualTo(user.getLastName()),
                o -> o.node("email").isEqualTo(user.getEmail()),
                o -> o.node("password").isAbsent(),
                o -> o.node("createdAt").isEqualTo(Utils.formatDate(user.getCreatedAt()))
        );
    }

    @Test
    public void testUpdateAnotherUser() throws Exception {
        var testUser = getNewSavedUser();
        var data = new HashMap<>();
        var firstName = faker.name().firstName();
        data.put("firstName", firstName);

        var request = put("/api/users/" + testUser.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSelfDestroy() throws Exception {
        var user = getNewSavedUser();
        var userToken = jwt().jwt(builder -> builder.subject(user.getEmail()));

        assertTrue(userRepository.existsById(user.getId()));

        var request = delete("/api/users/" + user.getId()).with(userToken);
        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(user.getId()));
    }

    @Test
    public void testDestroyAnotherUser() throws Exception {
        var user = getNewSavedUser();

        assertTrue(userRepository.existsById(user.getId()));

        var request = delete("/api/users/" + user.getId())
                .with(token);
        mockMvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateNotValid() throws Exception {
        var email = faker.internet().emailAddress();

        var data = new HashMap<>();
        data.put("email", email);

        var request = post("/api/users").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNotValid() throws Exception {
        var testUser = getNewSavedUser();
        var data = new HashMap<>();
        var password = faker.internet().password(1, 2);
        data.put("password", password);

        var request = put("/api/users/" + testUser.getId()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }
}
