package com.localtest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.localtest.service.WebSocketClientService;
import com.localtest.service.AccessKeyService;
import com.localtest.service.ProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TestController {
    private final WebSocketClientService wsService;
    private final AccessKeyService accessKeyService;
    private final ProtocolService protocolService;
    
    @Autowired
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appCommands", protocolService.getAppCommands());
        model.addAttribute("adcuCommands", protocolService.getAdcuCommands());
        return "index";
    }

    @PostMapping("/connect")
    @ResponseBody
    public String connect(@RequestParam String serverUrl, @RequestParam String deviceId, @RequestParam String deviceType) throws Exception {
        wsService.connect(serverUrl, deviceId, deviceType);
        return "OK";
    }

    @PostMapping("/send-command")
    @ResponseBody
    public String sendCommand(@RequestParam String deviceType, @RequestBody String payload) {
        wsService.sendCommand(deviceType, payload);
        return "OK";
    }

    @GetMapping("/latest")
    @ResponseBody
    public String latest(@RequestParam String deviceType) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("request", WebSocketClientService.getLastRequest(deviceType));
            result.put("response", WebSocketClientService.getLastResponse(deviceType));
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/protocols")
    @ResponseBody
    public String getProtocols() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode appArray = objectMapper.createArrayNode();
            ArrayNode adcuArray = objectMapper.createArrayNode();
            
            // APP 명령어들
            List<ProtocolService.ProtocolCommand> appCommands = protocolService.getAppCommands();
            for (ProtocolService.ProtocolCommand cmd : appCommands) {
                ObjectNode commandNode = objectMapper.createObjectNode();
                commandNode.put("name", cmd.getName());
                commandNode.put("description", cmd.getDescription());
                commandNode.put("json", cmd.getFormattedJson("TRACTOR_001"));
                appArray.add(commandNode);
            }
            
            // ADCU 명령어들
            List<ProtocolService.ProtocolCommand> adcuCommands = protocolService.getAdcuCommands();
            for (ProtocolService.ProtocolCommand cmd : adcuCommands) {
                ObjectNode commandNode = objectMapper.createObjectNode();
                commandNode.put("name", cmd.getName());
                commandNode.put("description", cmd.getDescription());
                commandNode.put("json", cmd.getFormattedJson("TRACTOR_001"));
                adcuArray.add(commandNode);
            }
            
            root.set("app", appArray);
            root.set("adcu", adcuArray);
            
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/send-protocol")
    @ResponseBody
    public String sendProtocol(@RequestParam String deviceType, @RequestParam String commandName) {
        System.out.println("=== sendProtocol 호출 ===");
        System.out.println("deviceType: " + deviceType);
        System.out.println("commandName: " + commandName);
        
        String sender = "APP".equalsIgnoreCase(deviceType) ? "APP" : "ADCU";
        System.out.println("sender: " + sender);
        
        ProtocolService.ProtocolCommand command = protocolService.findCommand(commandName, sender);
        System.out.println("found command: " + (command != null ? command.getName() : "null"));
        
        if (command != null) {
            String jsonCommand = command.getFormattedJson("TRACTOR_001");
            System.out.println("sending json: " + jsonCommand);
            wsService.sendCommand(deviceType, jsonCommand);
            return "OK";
        } else {
            return "Command not found: " + commandName + " for " + sender;
        }
    }

    // 연결 상태 확인 API
    @GetMapping("/connection-status")
    @ResponseBody
    public String getConnectionStatus(@RequestParam String deviceType) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            boolean isConnected = wsService.isConnected(deviceType);
            result.put("connected", isConnected);
            result.put("deviceType", deviceType);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // 연결 해제 API
    @PostMapping("/disconnect")
    @ResponseBody
    public String disconnect(@RequestParam String deviceType) {
        try {
            wsService.disconnect(deviceType);
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
