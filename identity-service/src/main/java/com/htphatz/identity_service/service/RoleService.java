package com.htphatz.identity_service.service;

import com.htphatz.identity_service.dto.request.RoleRequest;
import com.htphatz.identity_service.dto.response.RoleResponse;
import com.htphatz.identity_service.entity.Permission;
import com.htphatz.identity_service.entity.Role;
import com.htphatz.identity_service.mapper.RoleMapper;
import com.htphatz.identity_service.repository.PermissionRepository;
import com.htphatz.identity_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public RoleResponse createRole(RoleRequest request) {
        Role role = roleMapper.toRole(request);
        List<Permission> permissions = permissionRepository.findAllById(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toRoleResponse).collect(Collectors.toList());
    }

    public void deleteRole(String roleName) {
        roleRepository.deleteById(roleName);
    }
}
