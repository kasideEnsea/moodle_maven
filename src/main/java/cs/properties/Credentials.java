package cs.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Credentials {
    @Value("${moodle.username}")
    private String username;

    @Value("${moodle.password}")
    private String password;

    @Value("${moodle.course}")
    private String course;

    @Value("${moodle.imageSourse}")
    private String imageSourse;

    public String getUsername() {
        return username.trim();
    }

    public String getPassword() {
        return password.trim();
    }

    public String getCourse(){return course.trim();}

    public String getImageSourse(){return imageSourse.trim();}
}
