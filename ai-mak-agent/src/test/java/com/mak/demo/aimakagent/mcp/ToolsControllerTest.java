package com.mak.demo.aimakagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mak.demo.aimakagent.AiMakAgentApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ToolsControllerTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private final String base = "http://localhost:8089"; // 按你的应用端口改

    @Test
    void toolsList_shouldReturn200() throws Exception {
        String body = """
        {
          "jsonrpc": "2.0",
          "id": "1",
          "method": "tools/list",
          "params": {}
        }
        """.strip();

        String endpoint = pickEndpoint(); // 尝试 /mcp -> /mcp/sync
        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "HTTP 非 200: " + resp.body());
        assertTrue(resp.body().contains("\"jsonrpc\":\"2.0\""), resp.body());
        assertTrue(resp.body().contains("\"result\""), resp.body());
        System.out.println("OK: " + resp.body());
    }

    /** 优先试 /mcp；失败再试 /mcp/sync，便于兼容不同版本 */
    private String pickEndpoint() throws Exception {
        String[] candidates = { "/mcp", "/mcp/sync" };
        for (String path : candidates) {
            String url = base + path;
            HttpRequest ping = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                    {"jsonrpc":"2.0","id":"ping","method":"tools/list","params":{}}
                    """.strip()))
                    .build();
            try {
                int code = client.send(ping, HttpResponse.BodyHandlers.ofString()).statusCode();
                if (code != 404 && code != 405) return url; // 看起来是对的端点
            } catch (Exception ignore) {}
        }
        // 如果都不行，默认用 /mcp，让断言报出具体响应
        return base + "/mcp";
    }
}
