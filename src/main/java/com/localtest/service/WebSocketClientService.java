package com.localtest.service;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WebSocketClientService {
    private final AccessKeyService accessKeyService = new AccessKeyService();
    private final ConcurrentHashMap<String, WebSocketClient> clients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> lastRequestMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> lastResponseMap = new ConcurrentHashMap<>();

    public void connect(String serverUrl, String deviceId, String deviceType) throws Exception {
        String key = deviceType.toLowerCase();
        if (clients.containsKey(key) && clients.get(key).isOpen()) return;

        WebSocketClient client = new WebSocketClient(new URI(serverUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                sendInitialConnection(deviceId, deviceType);
            }

            @Override
            public void onMessage(String message) {
                lastResponseMap.put(key, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                lastResponseMap.put(key, "연결 종료: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                lastResponseMap.put(key, "오류: " + ex.getMessage());
            }
        };
        clients.put(key, client);
        client.connect();
    }

    private void sendInitialConnection(String deviceId, String deviceType) {
        try {
            String vinId = deviceId; // 테스트용
            String accessKey = accessKeyService.generateAccessKey(deviceId, vinId);
            String msg = String.format(
                "{\"deviceId\":\"%s\",\"command\":\"remote_connection\",\"requestType\":\"%s\",\"accessKey\":\"%s\"}",
                deviceId, deviceType, accessKey);
            lastRequestMap.put(deviceType.toLowerCase(), msg);
            clients.get(deviceType.toLowerCase()).send(msg);
        } catch (Exception e) {
            lastResponseMap.put(deviceType.toLowerCase(), "오류: " + e.getMessage());
        }
    }

    public void sendCommand(String deviceType, String jsonCommand) {
        if (clients.containsKey(deviceType.toLowerCase()) && clients.get(deviceType.toLowerCase()).isOpen()) {
            lastRequestMap.put(deviceType.toLowerCase(), jsonCommand);
            clients.get(deviceType.toLowerCase()).send(jsonCommand);
        }
    }

    public static String getLastRequest(String deviceType) {
        return lastRequestMap.getOrDefault(deviceType.toLowerCase(), "");
    }

    public static String getLastResponse(String deviceType) {
        return lastResponseMap.getOrDefault(deviceType.toLowerCase(), "");
    }
}
