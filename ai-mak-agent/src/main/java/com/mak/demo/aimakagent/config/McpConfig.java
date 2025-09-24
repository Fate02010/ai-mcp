package com.mak.demo.aimakagent.config;

import com.mak.demo.aimakagent.mcp.WeatherTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Fate.Mak
 * Created on 2025-09-24 15:08:50.
 * @version 1.0
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider tools(WeatherTools weatherTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)
                .build();
    }
}
