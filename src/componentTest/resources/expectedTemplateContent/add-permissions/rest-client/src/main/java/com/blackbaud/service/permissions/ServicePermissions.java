package com.blackbaud.service.permissions;

import com.blackbaud.security.permission.AbstractPermissions;

import java.util.Map;

public class ServicePermissions extends AbstractPermissions {
    @Override
    protected void initPermissions(Map<String, Long> permissions) {
        permissions.put("service.view", 1L);
        permissions.put("service.edit", 2L);
        permissions.put("service.delete", 3L);
    }
}
