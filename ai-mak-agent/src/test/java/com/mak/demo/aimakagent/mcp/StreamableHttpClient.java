package com.mak.demo.aimakagent.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 *
 * @author maijc8
 * Created on 2025-09-24 16:05:36.
 * @version 1.0
 */
public class StreamableHttpClient {

    public static void main(String[] args) {
        try {
            // 动态发现正确的端点
            String endpoint = discoverMcpEndpoint("http://localhost:8089");
            System.out.println("使用端点: " + endpoint);

            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(endpoint)
                    .build();

            var client = McpClient.sync(transport).build();

            client.initialize();

            client.ping();

            // List and demonstrate tools
//            McpSchema.ListToolsResult toolsList = client.listTools();
//            System.out.println("Available Tools = " + toolsList);

            McpSchema.CallToolResult weatherForcastResult = client.callTool(new McpSchema.CallToolRequest("toUpperCase",
                    Map.of("input", "hellow")));
            System.out.println("Weather Forcast: " + weatherForcastResult);

            client.closeGracefully();

        } catch (Exception e) {
            System.err.println("客户端执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 动态发现MCP服务端点
     */
    private static String discoverMcpEndpoint(String baseUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String[] candidates = { "/mcp", "/mcp/sync", "/ai/mcp", "/spring-ai/mcp" ,"/mcp/sse"};

        for (String path : candidates) {
            String url = baseUrl + path;
            HttpRequest ping = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                    {"jsonrpc":"2.0","id":"ping","method":"tools/list","params":{}}
                    """.strip()))
                    .build();

            try {
                HttpResponse<String> response = client.send(ping, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();

                // 如果返回200或其他非404/405错误，认为找到了正确端点
                if (code == 200 || (code != 404 && code != 405)) {
                    return url;
                }
            } catch (Exception ignore) {
                // 忽略连接异常，继续尝试下一个端点
            }
        }

        // 如果所有端点都失败，抛出异常
        throw new RuntimeException("无法找到可用的MCP端点。请检查服务器是否正确启动，并确认端点配置。");
    }
}
