package cs.client;

import com.fasterxml.jackson.core.JsonToken;
import cs.dto.DataDTO;
import cs.dto.RequestDTO;
import feign.Headers;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Map;

@FeignClient(value = "MoodleClient", url = "https://edu.vsu.ru/", configuration = MoodleClientConfiguration.class)
public interface MoodleClient {
    @GetMapping("/login/index.php")
    String getMoodleLoginPage();

    @GetMapping
    String loadAnything(URI baseUrl);

    @PostMapping(value = "/login/index.php", consumes = "application/x-www-form-urlencoded")
    String authorize(@RequestBody Map<String, ?> formData);

    @PostMapping(value = "/lib/ajax/service.php", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/plain;charset=UTF-8")
    RequestDTO[] getStudentList(@RequestParam("sesskey") String sk, @RequestParam("info") String info, @RequestBody String formData);
}
