package com.mak.demo.aimakagent.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 天气工具类
 * @author Fate.Mak
 * Created on 2025-09-24 11:36:24.
 * @version 1.0
 */
@Service
public class WeatherTools {


    // 简单请求/响应记录类型（会自动 JSON 序列化/反序列化）
    public record WeatherRequest(String city, String unit) {}
    public record WeatherReport(String city, String unit, String summary, double temperature) {}

    @Tool(
            name = "get_weather",
            description = "Get a fake current weather for a city. Params: city, unit(C/F)."
    )
    public WeatherReport getWeather(@ToolParam(description = "天气查询入参") WeatherRequest req) {

        var unit = (req.unit() == null || req.unit().isBlank()) ? "C" : req.unit().trim();
        // 这里为了演示写死一条数据，真实情况你可以调外部 API
        double temp = "F".equalsIgnoreCase(unit) ? 78.8 : 26.0;
        return new WeatherReport(req.city(), unit, "Sunny with light breeze", temp);
    }

    @Tool(
            name = "add",
            description = "Add two integers. Params: a, b."
    )
    public int add(AddRequest req) {
        return req.a() + req.b();
    }

    public record AddRequest(int a, int b) {}
}


