package cs.client;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import lombok.AllArgsConstructor;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class MoodleClientConfiguration {
    private ObjectFactory<HttpMessageConverters> messageConverters;

    @Bean
    @Primary
    Encoder feignFormEncoder() {
        return new FormEncoder(new SpringEncoder(this.messageConverters));
    }

    @Bean
    public OkHttpClient.Builder okHttpClientBuilder() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);


        return new OkHttpClient.Builder().cookieJar(new CookieJar() {
            private List<Cookie> state = Collections.emptyList();
            @Override
            public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                state = list;
            }

            @NotNull
            @Override
            public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                return state;
            }
        });
    }

}
