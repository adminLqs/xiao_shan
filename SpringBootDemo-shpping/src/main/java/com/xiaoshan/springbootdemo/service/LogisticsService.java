package com.xiaoshan.springbootdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.vo.LogisticsVO;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 物流服务类
 * 负责调用快递鸟API查询物流轨迹
 * 接口文档：https://api.kdniao.com/api/dist
 * 请求类型：8001（在途监控接口）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    // 快递鸟API正式环境地址（根据官方文档：https://api.kdniao.com/api/dist）
    @Value("${kdniao.api.url:https://api.kdniao.com/api/dist}")
    private String apiUrl;

    // 快递鸟用户ID（EBusinessID）
    @Value("${kdniao.app.key:1918612}")
    private String appKey;

    // 快递鸟API密钥（AppKey）- 保持原始格式包含横线
    @Value("${kdniao.app.secret:37691e1f-fa2b-4ce8-b0aa-1e5c3fd7dae3}")
    private String appSecret;

    // 请求类型：8001表示在途监控接口（即时查询）
    private static final String REQUEST_TYPE = "8001";

    // 日期时间格式化器
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // JSON解析工具
    private final ObjectMapper objectMapper;

    private final OrderMapper orderMapper;

    /**
     * 查询物流轨迹
     * 调用快递鸟API获取物流信息
     *
     * @param trackingNumber 物流单号
     * @param logisticsCode 物流公司代码（STO、YTO、JT等）
     * @return 物流信息VO
     */
    public LogisticsVO queryLogistics(String trackingNumber, String logisticsCode, Long orderId) {
        // 创建返回结果对象
        LogisticsVO result = new LogisticsVO();

        // 设置物流单号
        result.setTrackingNumber(trackingNumber);

        // 设置物流公司代码
        result.setLogisticsCode(logisticsCode);

        // 设置物流公司中文名称
        result.setLogisticsName(getLogisticsName(logisticsCode));

        try {
            // 去除物流单号中的空格和特殊字符
            String cleanTrackingNumber = trackingNumber.trim().replaceAll("\\s+", "");

            // 构建请求参数的JSON字符串
            String requestData = buildRequestData(cleanTrackingNumber, logisticsCode, orderId);

            log.info("========== 快递鸟API请求开始 ==========");
            log.info("请求URL: {}", apiUrl);
            log.info("EBusinessID: {}", appKey);
            log.info("RequestType: {}", REQUEST_TYPE);
            log.info("RequestData: {}", requestData);

            // 发送POST请求到快递鸟API
            String response = sendPostRequest(requestData);

            log.info("快递鸟响应: {}", response);
            log.info("========== 快递鸟API请求结束 ==========");

            // 解析快递鸟返回的JSON响应
            JsonNode jsonNode = objectMapper.readTree(response);

            // 判断请求是否成功
            boolean success = jsonNode.has("Success") && "true".equals(jsonNode.get("Success").asText());

            if (success) {
                // 创建轨迹列表
                List<LogisticsVO.TraceVO> traces = new ArrayList<>();

                // 获取轨迹数组
                JsonNode tracesNode = jsonNode.get("Traces");

                // 遍历每个轨迹节点
                if (tracesNode != null && tracesNode.isArray()) {
                    for (JsonNode traceNode : tracesNode) {
                        // 创建轨迹节点对象
                        LogisticsVO.TraceVO trace = new LogisticsVO.TraceVO();

                        // 获取轨迹发生时间
                        if (traceNode.has("AcceptTime")) {
                            String acceptTime = traceNode.get("AcceptTime").asText();
                            trace.setTime(parseDateTime(acceptTime));
                        }

                        // 获取轨迹描述（如：已签收、派送中）
                        if (traceNode.has("AcceptStation")) {
                            trace.setStatus(traceNode.get("AcceptStation").asText());
                        }

                        // 获取轨迹所在城市
                        if (traceNode.has("Location")) {
                            trace.setLocation(traceNode.get("Location").asText());
                        }

                        // 添加到轨迹列表
                        traces.add(trace);
                    }
                }

                // 设置轨迹列表到结果对象
                result.setTraces(traces);
                log.info("物流查询成功，获取到{}条轨迹", traces.size());
            } else {
                // 请求失败时记录失败原因
                String reason = jsonNode.has("Reason") ? jsonNode.get("Reason").asText() : "查询失败";
                log.error("物流查询失败: {}", reason);

                // 返回空轨迹列表
                result.setTraces(new ArrayList<>());
            }

        } catch (Exception e) {
            // 调用异常时记录错误日志
            log.error("调用物流API失败: {}", e.getMessage(), e);

            // 返回空轨迹列表
            result.setTraces(new ArrayList<>());
        }

        return result;
    }

    /**
     * 构建请求数据JSON字符串
     * 根据快递鸟8001接口文档构建请求参数
     *
     * @param trackingNumber 物流单号
     * @param logisticsCode 物流公司代码
     * @return JSON格式的请求数据
     */
    private String buildRequestData(String trackingNumber, String logisticsCode, Long orderId) {
        // 顺丰快递需要传递CustomerName参数（手机号后四位）
        if ("SF".equals(logisticsCode)|| "ZTO".equals(logisticsCode)) {
            // 需要从订单信息中获取收件人或寄件人手机号后四位
            Address address = orderMapper.findAddressByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("未查询到收件人字符串"));
            String phone  = address.getRecipientPhone();
            // 截取后四位
            String customerName = phone.substring(phone.length() - 4);

            return String.format("{\"ShipperCode\":\"%s\",\"LogisticCode\":\"%s\",\"CustomerName\":\"%s\"}",
                    logisticsCode, trackingNumber, customerName);
        }

        // 根据官方文档格式：{"ShipperCode":"STO","LogisticCode":"777357719949881"}
        // 注意：不需要传递OrderCode字段
        return String.format("{\"ShipperCode\":\"%s\",\"LogisticCode\":\"%s\"}",
                logisticsCode, trackingNumber);
    }

    /**
     * 发送POST请求到快递鸟API
     * 使用POST方式，参数放在Body中
     *
     * @param requestData 请求数据JSON
     * @return 快递鸟返回的响应字符串
     */
    private String sendPostRequest(String requestData) throws Exception {
        // 计算签名（快递鸟要求Base64编码的MD5）
        String dataSign = encrypt(requestData);

        log.debug("DataSign: {}", dataSign);

        // 构建POST请求参数（按照官方文档格式）
        StringBuilder params = new StringBuilder();
        params.append("RequestData=").append(URLEncoder.encode(requestData, "UTF-8"));
        params.append("&EBusinessID=").append(URLEncoder.encode(appKey, "UTF-8"));
        params.append("&RequestType=").append(REQUEST_TYPE);
        params.append("&DataSign=").append(URLEncoder.encode(dataSign, "UTF-8"));
        params.append("&DataType=2");

        log.debug("完整请求参数: {}", params.toString());

        // 创建URL对象
        URL url = new URL(apiUrl);

        // 打开HTTP连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法为POST
        connection.setRequestMethod("POST");

        // 设置连接超时时间（30秒）
        connection.setConnectTimeout(30000);

        // 设置读取超时时间（30秒）
        connection.setReadTimeout(30000);

        // 允许输出数据
        connection.setDoOutput(true);

        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        // 发送请求参数
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
            writer.write(params.toString());
            writer.flush();
        }

        // 获取HTTP响应状态码
        int responseCode = connection.getResponseCode();
        log.debug("HTTP响应码: {}", responseCode);

        // 读取响应内容
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    /**
     * 快递鸟签名算法
     * 规则：MD5(RequestData + AppSecret) 得到32位小写字符串，再对这个字符串做Base64编码
     *
     * @param requestData 请求数据JSON
     * @return Base64编码后的签名
     */
    private String encrypt(String requestData) throws Exception {
        // 拼接字符串（请求数据 + AppSecret）
        String toEncrypt = requestData + appSecret;
        log.debug("待加密字符串: {}", toEncrypt);

        // MD5加密（得到32位小写）
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(toEncrypt.getBytes("UTF-8"));

        // 转为十六进制字符串（小写）
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String md5Hex = hexString.toString().toLowerCase();
        log.debug("MD5结果(小写): {}", md5Hex);

        // 对MD5结果进行Base64编码
        String result = Base64.getEncoder().encodeToString(md5Hex.getBytes("UTF-8"));
        log.debug("Base64结果: {}", result);

        return result;
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateStr 日期时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     * @return LocalDateTime对象
     */
    private LocalDateTime parseDateTime(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) {
                return LocalDateTime.now();
            }
            return LocalDateTime.parse(dateStr, FORMATTER);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 根据物流公司代码获取中文名称
     *
     * @param code 物流公司代码
     * @return 物流公司中文名称
     */
    private String getLogisticsName(String code) {
        Map<String, String> nameMap = Map.of(
                "SF", "顺丰速运",
                "YTO", "圆通速递",
                "ZTO", "中通快递",
                "EMS", "邮政EMS",
                "YD", "韵达快递",
                "STO", "申通快递",
                "JT", "极兔速递",
                "JD", "京东物流",
                "HTKY", "百世快递"
        );
        return nameMap.getOrDefault(code, code);
    }
}