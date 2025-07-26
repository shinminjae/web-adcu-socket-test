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
    private static final ConcurrentHashMap<String, Boolean> connectionStatusMap = new ConcurrentHashMap<>();

    public void connect(String serverUrl, String deviceId, String deviceType) throws Exception {
        String key = deviceType.toLowerCase();
        if (clients.containsKey(key) && clients.get(key).isOpen()) return;

        WebSocketClient client = new WebSocketClient(new URI(serverUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("=== WebSocket 연결 성공 ===");
                System.out.println("deviceType: " + deviceType);
                System.out.println("handshake status: " + handshake.getHttpStatus());
                connectionStatusMap.put(key, true);
                sendInitialConnection(deviceId, deviceType);
            }

            @Override
            public void onMessage(String message) {
                lastResponseMap.put(key, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("=== WebSocket 연결 종료 ===");
                System.out.println("deviceType: " + deviceType);
                System.out.println("code: " + code + ", reason: " + reason);
                connectionStatusMap.put(key, false);
                lastResponseMap.put(key, "연결 종료: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("=== WebSocket 연결 오류 ===");
                System.out.println("deviceType: " + deviceType);
                System.out.println("error: " + ex.getMessage());
                connectionStatusMap.put(key, false);
                lastResponseMap.put(key, "오류: " + ex.getMessage());
            }
        };
        clients.put(key, client);
        connectionStatusMap.put(key, false); // 연결 시도 중
        client.connect();
        
        // 연결 완료를 잠시 기다림 (최대 3초)
        int maxWaitTime = 30; // 30 * 100ms = 3초
        int waitCount = 0;
        while (waitCount < maxWaitTime && !connectionStatusMap.getOrDefault(key, false)) {
            Thread.sleep(100);
            waitCount++;
        }
        
        System.out.println("=== 연결 시도 완료 ===");
        System.out.println("deviceType: " + deviceType);
        System.out.println("final connection status: " + connectionStatusMap.getOrDefault(key, false));
    }

    private void sendInitialConnection(String deviceId, String deviceType) {
        try {
            String vinId = deviceId; // 테스트용
            String accessKey = accessKeyService.generateAccessKey(deviceId, vinId);
            
            String msg;
            if ("app".equalsIgnoreCase(deviceType)) {
                // APP 초기 연결 메시지
                msg = String.format(
                    "{\"requestType\":\"app\",\"deviceId\":\"%s\",\"command\":\"remote_connection\",\"accessKey\":\"%s\"}",
                    deviceId, accessKey);
            } else {
                // ADCU 초기 연결 메시지
                msg = String.format(
                    "{\"deviceId\":\"%s\",\"command\":\"remote_connection\"}",
                    deviceId);
            }
            
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

    // 실제 연결 상태 확인 메서드
    public boolean isConnected(String deviceType) {
        String key = deviceType.toLowerCase();
        
        // connectionStatusMap에서 실제 연결 상태 확인
        Boolean connectionStatus = connectionStatusMap.get(key);
        
        // 연결 상태 로깅 추가
        System.out.println("=== 연결 상태 확인 ===");
        System.out.println("deviceType: " + deviceType);
        System.out.println("connectionStatusMap value: " + connectionStatus);
        System.out.println("client exists: " + clients.containsKey(key));
        
        if (clients.containsKey(key)) {
            WebSocketClient client = clients.get(key);
            boolean isOpen = client.isOpen();
            System.out.println("client.isOpen(): " + isOpen);
            System.out.println("connection state: " + client.getReadyState());
        }
        
        // connectionStatusMap이 true이거나, 클라이언트가 존재하고 열려있으면 연결됨
        return Boolean.TRUE.equals(connectionStatus) || 
               (clients.containsKey(key) && clients.get(key).isOpen());
    }

    // 연결 해제 메서드
    public void disconnect(String deviceType) {
        String key = deviceType.toLowerCase();
        if (clients.containsKey(key)) {
            WebSocketClient client = clients.get(key);
            if (client.isOpen()) {
                client.close();
            }
            clients.remove(key);
        }
    }
}
