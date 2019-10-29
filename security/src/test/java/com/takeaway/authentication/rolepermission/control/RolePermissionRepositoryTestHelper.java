package com.takeaway.authentication.rolepermission.control;

import com.takeaway.authentication.AbstractRepositoryTestHelper;
import com.takeaway.authentication.rolepermission.entity.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User: StMinko Date: 19.10.2019 Time: 17:44
 *
 * <p>
 */
@Component
@Transactional
public class RolePermissionRepositoryTestHelper extends AbstractRepositoryTestHelper<RolePermission, UUID, RolePermissionRepository>
{
  @Autowired
  public RolePermissionRepositoryTestHelper(RolePermissionRepository rolePermissionRepository)
  {
    super(rolePermissionRepository);
  }
}
