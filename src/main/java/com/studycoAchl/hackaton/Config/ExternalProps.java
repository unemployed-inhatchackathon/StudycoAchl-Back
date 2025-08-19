package com.studycoAchl.hackaton.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external")
public record ExternalProps(String key) {
}
