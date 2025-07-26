package com.localtest.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ProtocolService {
    
    public static class ProtocolCommand {
        private String name;
        private String sender;
        private String jsonTemplate;
        private String description;
        
        public ProtocolCommand(String name, String sender, String jsonTemplate, String description) {
            this.name = name;
            this.sender = sender;
            this.jsonTemplate = jsonTemplate;
            this.description = description;
        }
        
        // Getters
        public String getName() { return name; }
        public String getSender() { return sender; }
        public String getJsonTemplate() { return jsonTemplate; }
        public String getDescription() { return description; }
        
        public String getFormattedJson(String deviceId) {
            return jsonTemplate
                .replace("```json", "")
                .replace("```", "")
                .replace("${deviceId}", deviceId)
                .replace("${farmlandId}", "FARM_001")
                .replace("${waypointId}", "WAY_001")
                .replace("${mapId}", "MAP_001")
                .trim();
        }
    }
    
    private final List<ProtocolCommand> appCommands = new ArrayList<>();
    private final List<ProtocolCommand> adcuCommands = new ArrayList<>();
    
    public ProtocolService() {
        initializeProtocols();
    }
    
    private void initializeProtocols() {
        // Excel 문서에서 추출한 APP 명령어들
        appCommands.add(new ProtocolCommand("시작", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_go\" } ```", 
            "자율주행 시작"));
        
        appCommands.add(new ProtocolCommand("멈춤", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_stop\" } ```", 
            "자율주행 중지"));
        
        appCommands.add(new ProtocolCommand("홈 (복귀주행)", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_autodriving\", \"target\": \"home\" } ```", 
            "홈으로 복귀주행"));
        
        appCommands.add(new ProtocolCommand("경작지와 경로 선택", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_autodriving\", \"target\": \"work\", \"farmlandId\": \"${farmlandId}\", \"waypointId\": \"${waypointId}\" } ```", 
            "경작지와 경로를 선택하여 작업주행"));
        
        appCommands.add(new ProtocolCommand("경로 선택", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_map_selection\", \"mapId\": \"${mapId}\" } ```", 
            "경로 맵 선택"));
        
        appCommands.add(new ProtocolCommand("실시간 영상 요청", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"get_live_view\", \"target\": \"front\" } ```", 
            "전방 실시간 영상 요청"));
        
        appCommands.add(new ProtocolCommand("PTO 해제", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_pto_off\", \"log\": true } ```", 
            "PTO(Power Take-Off) 해제"));
        
        appCommands.add(new ProtocolCommand("경적", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"horn\", \"log\": true } ```", 
            "경적 울리기"));
        
        appCommands.add(new ProtocolCommand("비상등", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"hazard_lights\", \"log\": true } ```", 
            "비상등 켜기/끄기"));
        
        appCommands.add(new ProtocolCommand("작업 상태 조회", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"get_working_status\", \"log\": true } ```", 
            "현재 작업 상태 조회"));
        
        appCommands.add(new ProtocolCommand("헬스 체크", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"health_check\" } ```", 
            "시스템 상태 확인"));
        
        appCommands.add(new ProtocolCommand("초기 연결", "APP", 
            "```json { \"requestType\": \"app\", \"deviceId\": \"${deviceId}\", \"command\": \"remote_connection\" } ```", 
            "WebSocket 초기 연결"));
        
        // Excel 문서에서 추출한 ADCU 명령어들
        adcuCommands.add(new ProtocolCommand("실시간 위치", "ADCU", 
            "```json { \"responseType\": \"adcu\", \"deviceId\": \"${deviceId}\", \"command\": \"RealtimeLocation\", \"log\": true } ```", 
            "실시간 위치 정보 전송"));
        
        adcuCommands.add(new ProtocolCommand("차량 상태", "ADCU", 
            "```json { \"responseType\": \"adcu\", \"deviceId\": \"${deviceId}\", \"command\": \"vehicle_status\", \"lat\": \"37.5665\", \"lon\": \"126.9780\", \"message\": \"정상 운행 중\" } ```", 
            "차량 상태 정보 전송"));
        
        adcuCommands.add(new ProtocolCommand("초기 연결", "ADCU", 
            "```json { \"deviceId\": \"${deviceId}\", \"command\": \"remote_connection\" } ```", 
            "ADCU 초기 연결"));
    }
    
    public List<ProtocolCommand> getAppCommands() {
        return appCommands;
    }
    
    public List<ProtocolCommand> getAdcuCommands() {
        return adcuCommands;
    }
    
    public ProtocolCommand findCommand(String name, String sender) {
        System.out.println("=== findCommand 호출 ===");
        System.out.println("name: " + name);
        System.out.println("sender: " + sender);
        
        List<ProtocolCommand> commands = "APP".equals(sender) ? appCommands : adcuCommands;
        System.out.println("검색할 명령어 목록 크기: " + commands.size());
        
        for (ProtocolCommand cmd : commands) {
            System.out.println("명령어: " + cmd.getName() + " (매칭: " + cmd.getName().equals(name) + ")");
        }
        
        ProtocolCommand result = commands.stream()
            .filter(cmd -> cmd.getName().equals(name))
            .findFirst()
            .orElse(null);
            
        System.out.println("찾은 명령어: " + (result != null ? result.getName() : "null"));
        return result;
    }
} 