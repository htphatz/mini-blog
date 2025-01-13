package com.htphatz.identity_service.controller;

import com.htphatz.identity_service.dto.request.PermissionRequest;
import com.htphatz.identity_service.dto.response.APIResponse;
import com.htphatz.identity_service.dto.response.PermissionResponse;
import com.htphatz.identity_service.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping
    public APIResponse<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse result = permissionService.createPermission(request);
        return APIResponse.<PermissionResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> result = permissionService.getAllPermissions();
        return APIResponse.<List<PermissionResponse>>builder().result(result).build();
    }

    @DeleteMapping("{permissionName}")
    public APIResponse<Void> deletePermission(@PathVariable("permissionName") String permissionName) {
        permissionService.deletePermission(permissionName);
        return APIResponse.<Void>builder().build();
    }
}
