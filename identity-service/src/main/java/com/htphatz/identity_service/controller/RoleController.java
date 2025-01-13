package com.htphatz.identity_service.controller;

import com.htphatz.identity_service.dto.request.RoleRequest;
import com.htphatz.identity_service.dto.response.APIResponse;
import com.htphatz.identity_service.dto.response.RoleResponse;
import com.htphatz.identity_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    public APIResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse result = roleService.createRole(request);
        return APIResponse.<RoleResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> result = roleService.getAllRoles();
        return APIResponse.<List<RoleResponse>>builder().result(result).build();
    }

    @DeleteMapping("{roleName}")
    public APIResponse<Void> deleteRole(@PathVariable("roleName") String roleName) {
        roleService.deleteRole(roleName);
        return APIResponse.<Void>builder().build();
    }
}
