package com.compiletime.api;

import com.compiletime.application.BuildService;
import com.compiletime.domain.BuildSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/command")
public class CommandController {

    private final BuildService buildService;

    public CommandController(BuildService buildService) {
        this.buildService = buildService;
    }

    @PostMapping("/start")
    public BuildSession start(@RequestParam String cmd) {
        return buildService.startBuild(cmd);
    }
}
