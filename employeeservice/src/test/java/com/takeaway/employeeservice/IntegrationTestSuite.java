package com.takeaway.employeeservice;

import com.takeaway.employeeservice.department.api.dto.DepartmentRequestTestFactory;
import com.takeaway.employeeservice.department.service.DepartmentParameterTestFactory;
import com.takeaway.employeeservice.employee.api.dto.CreateEmployeeRequestTestFactory;
import com.takeaway.employeeservice.employee.api.dto.UpdateEmployeeRequestTestFactory;
import com.takeaway.employeeservice.employee.service.EmployeeEventPublisherCapable;
import com.takeaway.employeeservice.employee.service.EmployeeParameterTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * User: StMinko
 * Date: 18.03.2019
 * Time: 12:57
 * <p/>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = { EmployeeServiceApplication.class })
public abstract class IntegrationTestSuite
{
    // =========================== Class Variables ===========================
    // =============================  Variables  =============================

    @Autowired
    protected DepartmentParameterTestFactory departmentParameterTestFactory;

    @Autowired
    protected EmployeeParameterTestFactory employeeParameterTestFactory;

    @Autowired
    protected DepartmentRequestTestFactory departmentRequestTestFactory;

    @Autowired
    protected CreateEmployeeRequestTestFactory createEmployeeRequestTestFactory;

    @Autowired
    protected UpdateEmployeeRequestTestFactory updateEmployeeRequestTestFactory;

    @SpyBean
    protected EmployeeEventPublisherCapable employeeEventPublisher;

    // ============================  Constructors  ===========================
    // ===========================  public  Methods  =========================

    @BeforeEach
    void setUp()
    {
        doNothing().when(employeeEventPublisher)
                   .employeeCreated(any());
        doNothing().when(employeeEventPublisher)
                   .employeeDeleted(any());
        doNothing().when(employeeEventPublisher)
                   .employeeUpdated(any());
    }

    // =================  protected/package local  Methods ===================
    // ===========================  private  Methods  ========================
    // ============================  Inner Classes  ==========================
    // ============================  End of class  ===========================
}
