package com.mak.demo.aimakclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class McpClientDemo {

    private static final String BASE = "http://localhost:8089";   // 按需修改
    private static final String SSE_PATH = "/sse";                 // 按需加上 context-path

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 1) 建立 SSE，获得 message 端点（包含 sessionId）
        CompletableFuture<String> endpointFuture = new CompletableFuture<>();
        CompletableFuture<Void> ssePump = openSseAndListen(client, endpointFuture);

        String messageEndpoint = endpointFuture.get(10, TimeUnit.SECONDS);
        System.out.println("[OK] message endpoint = " + messageEndpoint);

        // 2) tools/list
        String listBody = """
                {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
                """;
        String listResp = postJson(client, messageEndpoint, listBody);
        System.out.println("tools/list response:\n" + listResp);

        // 3) tools/call（使用你给的请求体）
        String callBody = """
                {
                  "method": "tools/call",
                  "params": {
                    "name": "get_weather",
                    "arguments": {
                      "req": {
                        "city": "dd",
                        "unit": "dd"
                      }
                    },
                    "_meta": { "progressToken": 3 }
                  },
                  "jsonrpc": "2.0",
                  "id": 3
                }
                """;
        String callResp = postJson(client, messageEndpoint, callBody);
        System.out.println("tools/call response:\n" + callResp);

        // SSE 还在后台线程监听进度推送；此处等待几秒以便观察
        Thread.sleep(3000);

        // 结束 Demo（真实长连可常驻监听）
        ssePump.cancel(true);
        System.out.println("Done.");
    }

    /**
     * 建立 SSE 连接并在后台线程解析事件。
     * - 解析到首个 event=endpoint 的 data，拼成完整 URL，complete 给 endpointFuture
     * - 其余事件（如进度/日志）直接打印
     */
    private static CompletableFuture<Void> openSseAndListen(HttpClient client, CompletableFuture<String> endpointFuture) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + SSE_PATH))
                .GET()
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(30))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
                .thenAcceptAsync(resp -> {
                    if (resp.statusCode() != 200) {
                        endpointFuture.completeExceptionally(
                                new IllegalStateException("SSE handshake HTTP " + resp.statusCode()));
                        return;
                    }
                    System.out.println("[SSE] connected. parsing events...");
                    try (var br = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                        String line;
                        String event = "message";
                        StringBuilder dataBuf = new StringBuilder();

                        while ((line = br.readLine()) != null) {
                            if (line.isEmpty()) {
                                // dispatch one event
                                String data = dataBuf.toString();
                                if ("endpoint".equals(event)) {
                                    String endpointPath = data.trim();
                                    // 兼容可能返回 JSON 的情况（简易提取）
                                    if (endpointPath.startsWith("{")) {
                                        int i = endpointPath.indexOf("endpoint");
                                        if (i >= 0) {
                                            int q = endpointPath.indexOf(':', i);
                                            int s = endpointPath.indexOf('"', q + 1);
                                            int e = endpointPath.indexOf('"', s + 1);
                                            if (s > 0 && e > s) {
                                                endpointPath = endpointPath.substring(s + 1, e);
                                            }
                                        }
                                    }
                                    if (!endpointPath.startsWith("/")) {
                                        // 防御性兜底：如果是相对段，转为标准
                                        endpointPath = "/" + endpointPath;
                                    }
                                    String full = BASE + endpointPath;
                                    if (!endpointFuture.isDone()) {
                                        endpointFuture.complete(full);
                                    }
                                    System.out.println("[SSE] endpoint => " + full);
                                } else {
                                    // 其他事件（如 progress/log/telemetry）
                                    System.out.println("[SSE][" + event + "] " + data);
                                }
                                // reset
                                event = "message";
                                dataBuf.setLength(0);
                                continue;
                            }

                            if (line.startsWith(":")) {
                                // 注释行，忽略
                                continue;
                            }
                            if (line.startsWith("event:")) {
                                event = line.substring(6).trim();
                                continue;
                            }
                            if (line.startsWith("data:")) {
                                if (dataBuf.length() > 0) dataBuf.append('\n');
                                dataBuf.append(line.substring(5).trim());
                            }
                            // 也可以处理 id:, retry: 等字段，这里省略
                        }
                    } catch (IOException e) {
                        if (!endpointFuture.isDone()) {
                            endpointFuture.completeExceptionally(e);
                        }
                    }
                });
    }

    private static String postJson(HttpClient client, String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
        }
        return res.body();
    }

    // 如果你需要自己构造 URL 上的 query（备用小工具）
    @SuppressWarnings("unused")
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}