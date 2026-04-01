package com.arthexis.platform.transfer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OcppFtpServerProperties.class)
public class OcppFtpServerConfig {}
