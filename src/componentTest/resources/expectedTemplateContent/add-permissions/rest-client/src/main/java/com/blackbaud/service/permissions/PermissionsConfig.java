package com.blackbaud.service.permissions;

import com.blackbaud.security.permission.PermissionRegistry;
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
                new ServicePermission()
        )
    }
}
