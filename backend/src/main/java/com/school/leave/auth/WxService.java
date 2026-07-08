package com.school.leave.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.leave.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 微信小程序登录：code → jscode2session → openid。
 * 未配置 WX_APPID/WX_SECRET 时 enabled()=false，前端隐藏微信登录入口。
 * AppSecret 仅从环境变量读取，绝不落库/日志/响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    @Value("${wx.appid:}")
    private String appid;

    @Value("${wx.secret:}")
    private String secret;

    public boolean enabled() {
        return StringUtils.hasText(appid) && StringUtils.hasText(secret);
    }

    /** code 换 openid；微信侧错误统一 5002 */
    public String codeToOpenid(String code) {
        if (!enabled()) throw BizException.wxUnavailable("微信登录未启用");
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code"
                .formatted(appid, secret, UriUtils.encode(code, StandardCharsets.UTF_8));
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            String body = HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("openid")) return node.get("openid").asText();
            log.warn("jscode2session 失败: errcode={} errmsg={}",
                    node.path("errcode").asInt(), node.path("errmsg").asText());
            throw BizException.wxUnavailable("微信登录失败: " + node.path("errmsg").asText("未知错误"));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("jscode2session 异常: {}", e.toString());
            throw BizException.wxUnavailable("微信服务暂不可用");
        }
    }
}
