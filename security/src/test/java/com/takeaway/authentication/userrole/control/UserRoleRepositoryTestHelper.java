package com.takeaway.authentication.userrole.control;

import com.takeaway.authentication.AbstractRepositoryTestHelper;
import com.takeaway.authentication.userrole.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User: StMinko
 * Date: 30.10.2019
 * Time: 15:49
 * <p/>
 */
@Component
@Transactional
public class UserRoleRepositoryTestHelper extends AbstractRepositoryTestHelper<UserRole, UUID, UserRoleRepository>
{
    @Autowired
    public UserRoleRepositoryTestHelper(UserRoleRepository repository)
    {
        super(repository);
    }
}