package com.blackbaud.service.core;

import com.blackbaud.security.permission.PermissionsRegistry;
import com.blackbaud.service.permissions.ServicePermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class PermissionsConfig {
    @Autowired
    private PermissionsRegistry permissionsRegistry;

    @PostConstruct
    private void registerPermissions() {
        permissionsRegistry.registerPermissionConfiguration(
            new ServicePermissions()
        );
    }
}
