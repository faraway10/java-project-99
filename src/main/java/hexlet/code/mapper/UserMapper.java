package hexlet.code.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;

import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.dto.user.UserDTO;
import hexlet.code.dto.user.UserUpdateDTO;
import hexlet.code.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {
    @Autowired
    private PasswordEncoder encoder;

    public abstract User map(UserCreateDTO model);

    public abstract UserDTO map(User model);

    public abstract User map(UserDTO dto);

    public abstract void update(UserUpdateDTO dto, @MappingTarget User model);

    @BeforeMapping
    final void encryptPassword(UserCreateDTO dto) {
        var password = dto.getPassword();
        dto.setPassword(encoder.encode(password));
    }

    @BeforeMapping
    final void encryptPassword(UserUpdateDTO dto) {
        var password = dto.getPassword();
        if (password != null) {
            dto.setPassword(JsonNullable.of(encoder.encode(password.get())));
        }
    }
}
