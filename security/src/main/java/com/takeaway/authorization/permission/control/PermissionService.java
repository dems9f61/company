package com.takeaway.authorization.permission.control;

import com.takeaway.authorization.permission.entity.Permission;
import com.takeaway.authorization.runtime.auditing.boundary.AbstractDefaultAuditedEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.UUID;

/**
 * User: StMinko Date: 17.10.2019 Time: 11:49
 *
 * <p>
 */
@Service
public class PermissionService extends AbstractDefaultAuditedEntityService<PermissionRepository, Permission, UUID>
{
    @Autowired
    public PermissionService(PermissionRepository repository, Validator validator)
    {
        super(repository, validator);
    }
}
