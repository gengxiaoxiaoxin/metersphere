package io.metersphere.sdk.service;

import io.metersphere.sdk.dto.Permission;
import io.metersphere.sdk.dto.PermissionDefinitionItem;
import io.metersphere.sdk.dto.request.PermissionSettingUpdateRequest;
import io.metersphere.sdk.util.PermissionCache;
import io.metersphere.system.domain.UserRole;
import io.metersphere.system.mapper.UserRoleMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author jianxing
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseUserRoleService {
    public static final String SYSTEM_TYPE = "SYSTEM";
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private BaseUserRolePermissionService baseUserRolePermissionService;

    /**
     * 根据用户组获取对应的权限配置项
     * @param userRole
     * @return
     */
    public List<PermissionDefinitionItem> getPermissionSetting(UserRole userRole) {
        // 获取该用户组拥有的权限
        Set<String> permissionIds = baseUserRolePermissionService.getPermissionIdSetByRoleId(userRole.getId());
        // 获取所有的权限
        List<PermissionDefinitionItem> permissionDefinition = permissionCache.getPermissionDefinition();
        // 过滤该用户组级别的菜单，例如系统级别
        permissionDefinition = permissionDefinition.stream()
                .filter(item -> StringUtils.equals(item.getType(), userRole.getType()))
                .toList();

        // 设置勾选项
        permissionDefinition.forEach(firstLevel -> {
            List<PermissionDefinitionItem> children = firstLevel.getChildren();
            boolean allCheck = true;
            for (PermissionDefinitionItem secondLevel : children) {
                List<Permission> permissions = secondLevel.getPermissions();
                if (CollectionUtils.isEmpty(permissions)) {
                    continue;
                }
                boolean secondAllCheck = true;
                for (Permission p : permissions) {
                    if (permissionIds.contains(p.getId())) {
                        p.setEnable(true);
                    } else {
                        // 如果权限有未勾选，则二级菜单设置为未勾选
                        p.setEnable(false);
                        secondAllCheck = false;
                    }
                }
                secondLevel.setEnable(secondAllCheck);
                if (!secondAllCheck) {
                    // 如果二级菜单有未勾选，则一级菜单设置为未勾选
                    allCheck = false;
                }
            }
            firstLevel.setEnable(allCheck);
        });

        return permissionDefinition;
    }

    /**
     * 更新单个用户组的配置项
     * @param request
     */
    protected void updatePermissionSetting(PermissionSettingUpdateRequest request) {
        baseUserRolePermissionService.updatePermissionSetting(request);
    }

    protected UserRole add(UserRole userRole) {
        userRole.setId(UUID.randomUUID().toString());
        userRole.setCreateTime(System.currentTimeMillis());
        userRole.setUpdateTime(System.currentTimeMillis());
        userRoleMapper.insert(userRole);
        return userRole;
    }

    protected UserRole update(UserRole userRole) {
        userRole.setCreateUser(null);
        userRole.setCreateTime(null);
        userRole.setUpdateTime(System.currentTimeMillis());
        userRoleMapper.updateByPrimaryKeySelective(userRole);
        return userRole;
    }
}
