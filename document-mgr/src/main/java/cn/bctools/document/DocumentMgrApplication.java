package cn.bctools.document;

import cn.bctools.common.constant.SysConstant;
import cn.bctools.common.utils.SystemThreadLocal;
import cn.bctools.oauth2.annotation.EnableJvsMgrResourceServer;
import feign.RequestInterceptor;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.codec.Base64;

import java.io.UnsupportedEncodingException;
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



    private String getAuthorizationHeader(String clientId, String clientSecret) {

        if (clientId == null || clientSecret == null) {
        }

        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }

    @Bean
    public RequestInterceptor clientRequestInterceptor(ResourceServerProperties clientSecret) {
        return (requestTemplate) -> {
            requestTemplate.header(SysConstant.TENANTID, String.valueOf(SystemThreadLocal.get(SysConstant.TENANTID).toString()));
            requestTemplate.header(SysConstant.VERSION, String.valueOf(SystemThreadLocal.get(SysConstant.VERSION).toString()));
            //加请求头将凭证加到请求头上
            requestTemplate.header("Authorization", getAuthorizationHeader(clientSecret.getClientId(), clientSecret.getClientSecret()));
        };
    }
}

