package cs;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import cs.service.MoodleLoaderService;

import java.net.HttpURLConnection;

@Service
@SpringBootApplication
@AllArgsConstructor
@EnableFeignClients
public class MoodleLoaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoodleLoaderApplication.class, args);
    }

    private final MoodleLoaderService moodleLoader;

    @Bean
    public CommandLineRunner run() {
        return args -> {
            moodleLoader.loadMoodle();
        };
    }

}
