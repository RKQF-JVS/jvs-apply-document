package cn.bctools.document;

import cn.bctools.oauth2.annotation.EnableJvsMgrResourceServer;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.TimeUnit;

/**
 * @author guojing
 */
@RefreshScope
@EnableAsync
@EnableJvsMgrResourceServer
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
public class DocumentMgrApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentMgrApplication.class, args);
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(RestClientBuilder restClientBuilder) {
        // 处理当服务端因为超时或者其他原因关闭session，客户端仍然认为长连接存在，抛出异常“Connection reset by peer”
        restClientBuilder.setHttpClientConfigCallback(config -> config.setKeepAliveStrategy((httpResponse, httpContext) -> TimeUnit.MINUTES.toMinutes(3)));
        return new RestHighLevelClient(restClientBuilder);
    }
}

