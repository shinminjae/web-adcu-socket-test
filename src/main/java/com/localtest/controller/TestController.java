package com.localtest.controller;

import com.localtest.service.WebSocketClientService;
import com.localtest.service.AccessKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TestController {
    private final WebSocketClientService wsService;
    private final AccessKeyService accessKeyService;

    @GetMapping("/")
    public String index() {
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
        return "{"
                + "\"request\":\"" + WebSocketClientService.getLastRequest(deviceType) + "\","
                + "\"response\":\"" + WebSocketClientService.getLastResponse(deviceType) + "\""
                + "}";
    }
}
