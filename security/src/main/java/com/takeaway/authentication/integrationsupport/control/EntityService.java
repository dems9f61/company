package com.takeaway.authentication.integrationsupport.control;

import com.takeaway.authentication.integrationsupport.boundary.ApiResponsePage;
import com.takeaway.authentication.integrationsupport.boundary.DataView;
import com.takeaway.authentication.integrationsupport.entity.ServiceException;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * User: StMinko
 * Date: 17.09.2019
 * Time: 12:57
 * <p/>
 */
@Validated
@Transactional(propagation = Propagation.REQUIRED)
public interface EntityService<ENTITY, ID>
{
    // =========================== Class Variables ===========================
    // ==============================  Methods  ==============================

    @Transactional(propagation = Propagation.SUPPORTS)
    ApiResponsePage<ENTITY> findAll(Pageable pageable);

    @Transactional(propagation = Propagation.SUPPORTS)
    Optional<ENTITY> findById(@NotNull ID id);

    ENTITY create(@NotNull ENTITY entity) throws ServiceException;

    ENTITY update(@NotNull ID id, @NotNull ENTITY entity, Class<? extends DataView> validationGroup) throws ServiceException;

    void delete(@NotNull ID id) throws ServiceException;

    void deleteAll();

    // ============================  Inner Classes  ==========================
    // ============================  End of class  ===========================
}