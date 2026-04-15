package com.compiletime.api;

import com.compiletime.application.BuildService;
import com.compiletime.domain.BuildSession;
import org.springframework.http.ResponseEntity;
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

    /**
     * Shell hook fires this when a command starts.
     * cmd is base64-encoded by the shell hook to handle spaces safely.
     */
    @PostMapping("/start")
    public ResponseEntity<BuildSession> start(@RequestParam String cmd) {
        BuildSession session = buildService.startBuild(cmd);
        if (session == null) {
            // Not a build command — acknowledged but nothing to return
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * Shell hook fires this when a command exits.
     * exit is the shell exit code (0 = success).
     */
    @PostMapping("/done")
    public ResponseEntity<BuildSession> done(@RequestParam(defaultValue = "0") int exit) {
        BuildSession session = buildService.endBuild(exit);
        if (session == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }
}
