package com.compiletime.application;

import com.compiletime.config.DetectionProperties;
import org.springframework.stereotype.Component;

/**
 * Decides whether a terminal command is a build/wait command that should
 * trigger the quiz overlay.
 *
 * Matching rule: the incoming command must START WITH one of the known
 * build command prefixes (case-insensitive). This means:
 *   "npm run build --watch"  → matches "npm run build" ✓
 *   "npm run lint"           → does NOT match "npm run build" ✗
 */
@Component
public class CommandClassifier {

    private final DetectionProperties props;

    public CommandClassifier(DetectionProperties props) {
        this.props = props;
    }

    public boolean isBuildCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return false;
        }

        String cmd = rawCommand.trim().toLowerCase();

        return props.getBuildCommands().stream()
                .map(String::toLowerCase)
                .anyMatch(cmd::startsWith);
    }
}
