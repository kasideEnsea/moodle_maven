package cs.service;

import com.ibm.icu.text.Transliterator;
import cs.dto.DataDTO;
import cs.dto.RequestDTO;
import cs.entity.Student;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cs.client.MoodleClient;
import cs.exception.FatalException;
import cs.properties.Credentials;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@AllArgsConstructor
public class MoodleLoaderService {
    private final MoodleClient moodleClient;
    private final Credentials credentials;

    private static final String LOGIN_TOKEN_REGEXP = "<input type=\"hidden\" name=\"logintoken\" value=\"([a-zA-Z0-9]{32})\">";
    private static final Pattern LOGIN_TOKEN_PATTERN = Pattern.compile(LOGIN_TOKEN_REGEXP);
    private static final String SESSION_KEY_REGEXP = "\"sesskey\":\"([a-zA-Z0-9]{10})\"";
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile(SESSION_KEY_REGEXP);
    private static final String PROFILE_REGEXP = "Личный кабинет пользователя";
    private static  final Pattern PROFILE_PATTERN = Pattern.compile(PROFILE_REGEXP, Pattern.CANON_EQ);
    private static final String USERNAME_REGEXP = "<span class=\"usertext mr-1\">([a-zA-Zа-яА-Я ]+)</span>";
    private static  final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEXP, Pattern.CANON_EQ);
    private static final String INFO = "core_table_get_dynamic_table_content";
    private static final String STUDENT_REGEXP = "/>([- \\p{L}]+)</a></th><td";
    private static  final Pattern STUDENT_PATTERN = Pattern.compile(STUDENT_REGEXP);
    private static final String IMAGE_REGEXP = "src=\"([:./=?a-z0-9]+)\"";
    private static  final Pattern IMAGE_PATTERN = Pattern.compile(IMAGE_REGEXP);
    public static final String CYRILLIC_TO_LATIN = "Cyrillic-Latin";

    public void loadMoodle() throws UnsupportedEncodingException {
        String loginToken = getLoginToken();
        log.info("Login token: {}", loginToken);

        String sessionKey = getSessionKey(loginToken);
        log.info("Session key: {}", sessionKey);

        getStudents(sessionKey);

    }

    private String getLoginToken() {
        String rawData = moodleClient.getMoodleLoginPage();
        return extractTokenWithPattern(rawData, LOGIN_TOKEN_PATTERN);
    }

    private String getSessionKey(String loginToken) {
        Map<String, String> formData = getFormData(loginToken);
        String rawData = moodleClient.authorize(formData);
        String sessKey = extractTokenWithPattern(rawData, SESSION_KEY_PATTERN);
        ensureAuthorized(rawData);
        return sessKey;
    }

    private void ensureAuthorized(String rawData) {
        Matcher matcher = PROFILE_PATTERN.matcher(rawData);
        if (!matcher.find()) {
            throw new FatalException("Authorization attempt failed");
        }
        log.info("User is for sure authorized");
        log.info("User's full name: {}", extractTokenWithPattern(rawData, USERNAME_PATTERN));
    }

    private Map<String, String> getFormData(String loginToken) {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("logintoken", loginToken);
        formData.put("username", credentials.getUsername());
        formData.put("password", credentials.getPassword());
        return formData;
    }

    private String extractTokenWithPattern(String rawData, Pattern pattern) {
        Matcher matcher = pattern.matcher(rawData);
        if (!matcher.find()) {
            throw new FatalException("Unable to find token");
        }
        return matcher.group(1);
    }

    private LinkedList<Student> getStudents(String sessionKey) {
        String data = getStudentRequest(credentials.getCourse());
        RequestDTO rDTO = moodleClient.getStudentList(sessionKey, INFO, data)[0];
        String studentData = (rDTO).getData().getHtml();
        LinkedList<String> names = extractStringListWithPattern(studentData, STUDENT_PATTERN);
        LinkedList<String> urls = extractStringListWithPattern(studentData, IMAGE_PATTERN);
        LinkedList<Student> students = new LinkedList<>();
        for(int i=0; i<names.size(); i++){
            String name = names.get(i);
            String url = urls.get(i).replace("f2", "f3");
            String nameWOSpaces = name.replaceAll("\\s","");
            String result = transliterate(nameWOSpaces);
            String path = (credentials.getImageSourse()+result+".jpg");
            downloadFiles(url, path, 100000);
            students.add(new Student(name, path));
        }
        return students;
    }

    private String getStudentRequest(String course){
        return ("[{\"index\":0,\"methodname\":\"core_table_get_dynamic_table_content\",\"args\":" +
                "{\"component\":\"core_user\",\"handler\":\"participants\",\"uniqueid\":\"user-index-participants-"+course+"\"," +
                "\"sortdata\":[{\"sortby\":\"lastname\",\"sortorder\":4}],\"jointype\":1,\"filters\":{\"courseid\":" +
                "{\"name\":\"courseid\",\"jointype\":1,\"values\":["+course+"]}},\"firstinitial\":\"\",\"lastinitial\":" +
                "\"\",\"pagenumber\":\"1\",\"pagesize\":\"5000\",\"hiddencolumns\":[],\"resetpreferences\":false}}]");
    }

    private LinkedList<String> extractStringListWithPattern(String rawData, Pattern pattern) {
        LinkedList<String> allMatches = new LinkedList<>();
        Matcher matcher = pattern.matcher(rawData);
        while (matcher.find()) {
            allMatches.add(matcher.group(1));
        }
        return allMatches;
    }

    public static void downloadFiles(String strURL, String strPath, int buffSize)  {
        try {
            URL connection = new URL(strURL);
            HttpURLConnection urlconn;
            urlconn = (HttpURLConnection) connection.openConnection();
            urlconn.setRequestMethod("GET");
            urlconn.connect();
            InputStream in = null;
            in = urlconn.getInputStream();
            OutputStream writer = new FileOutputStream(strPath);
            byte[] buffer = new byte[buffSize];
            int c = in.read(buffer);
            while (c > 0) {
                writer.write(buffer, 0, c);
                c = in.read(buffer);
            }
            writer.flush();
            writer.close();
            in.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String transliterate(String message){
        char[] abcCyr =   {' ','а','б','в','г','д','е','ё', 'ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х', 'ц','ч', 'ш','щ','ъ','ы','ь','э', 'ю','я','А','Б','В','Г','Д','Е','Ё', 'Ж','З','И','Й','К','Л','М','Н','О','П','Р','С','Т','У','Ф','Х', 'Ц', 'Ч','Ш', 'Щ','Ъ','Ы','Ь','Э','Ю','Я','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
        String[] abcLat = {" ","a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch", "","i", "","e","ju","ja","A","B","V","G","D","E","E","Zh","Z","I","Y","K","L","M","N","O","P","R","S","T","U","F","H","Ts","Ch","Sh","Sch", "","I", "","E","Ju","Ja","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            for (int x = 0; x < abcCyr.length; x++ ) {
                if (message.charAt(i) == abcCyr[x]) {
                    builder.append(abcLat[x]);
                }
            }
        }
        return builder.toString();
    }

}
