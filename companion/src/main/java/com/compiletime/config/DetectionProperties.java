package com.compiletime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "compiletime.detection")
public class DetectionProperties {

    private int debounceSeconds = 3;
    private List<String> buildCommands = List.of();

    public int getDebounceSeconds() {
        return debounceSeconds;
    }

    public void setDebounceSeconds(int debounceSeconds) {
        this.debounceSeconds = debounceSeconds;
    }

    public List<String> getBuildCommands() {
        return buildCommands;
    }

    public void setBuildCommands(List<String> buildCommands) {
        this.buildCommands = buildCommands;
    }
}
