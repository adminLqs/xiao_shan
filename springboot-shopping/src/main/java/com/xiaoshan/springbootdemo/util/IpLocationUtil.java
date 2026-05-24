package com.xiaoshan.springbootdemo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * IP定位工具类
 * 使用 ip-api.com 免费接口获取 IP 属地信息
 */
@Slf4j
@Component
public class IpLocationUtil {

    private static final String IP_API_URL = "http://ip-api.com/json/";
    private static final int TIMEOUT = 5000;

    private final ObjectMapper objectMapper;

    public IpLocationUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 IP 属地信息
     * @param ip IP地址
     * @return 属地信息（如"广东广州"），获取失败返回 null
     */
    public String getLocation(String ip) {
        if (ip == null || ip.trim().isEmpty() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return null;
        }

        try {
            String apiUrl = IP_API_URL + ip + "?lang=zh-CN";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("IP API 请求失败，响应码: {}", responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonNode jsonNode = objectMapper.readTree(response.toString());
            String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";

            if ("success".equals(status)) {
                String regionName = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
                String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
                String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";

                if (!regionName.isEmpty() && !city.isEmpty()) {
                    return regionName + city;
                } else if (!regionName.isEmpty()) {
                    return regionName;
                } else if (!country.isEmpty()) {
                    return country;
                }
            } else {
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "";
                log.warn("IP API 返回失败: {}", message);
            }

        } catch (Exception e) {
            log.error("获取 IP 属地失败: ip={}, error={}", ip, e.getMessage());
        }

        return null;
    }
}
