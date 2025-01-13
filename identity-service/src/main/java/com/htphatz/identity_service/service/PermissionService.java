package com.htphatz.identity_service.service;

import com.htphatz.identity_service.dto.request.PermissionRequest;
import com.htphatz.identity_service.dto.response.PermissionResponse;
import com.htphatz.identity_service.entity.Permission;
import com.htphatz.identity_service.mapper.PermissionMapper;
import com.htphatz.identity_service.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionResponse createPermission(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        return permissionMapper.toPermissionResponse(permissionRepository.save(permission));
    }

    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).collect(Collectors.toList());
    }

    public void deletePermission(String permissionName) {
        permissionRepository.deleteById(permissionName);
    }
}
