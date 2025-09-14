package com.example.common.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "transfer.rest")
public class RestTransferProperties extends AbstractRestTransferProperties {
    // Inherit all properties from AbstractRestTransferProperties
    // Add any additional REST-specific properties here if needed
}
