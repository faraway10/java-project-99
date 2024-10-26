package hexlet.code.config;

import hexlet.code.util.Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncodersConfig {
    @Bean
    public Encoder passwordEncoder() {
        return new Encoder();
    }
}
