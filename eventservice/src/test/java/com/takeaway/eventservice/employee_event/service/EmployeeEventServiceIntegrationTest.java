package com.takeaway.eventservice.employee_event.service;

import com.takeaway.eventservice.IntegrationTestSuite;
import com.takeaway.eventservice.messaging.EmployeeEvent;
import com.takeaway.eventservice.messaging.dto.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: StMinko
 * Date: 20.03.2019
 * Time: 19:00
 * <p/>
 */
@DisplayName("Integration tests for employee event service")
class EmployeeEventServiceIntegrationTest extends IntegrationTestSuite
{
    @Autowired
    EmployeeEventRepository employeeEventRepository;

    @Autowired
    private EmployeeEventService employeeEventService;

    @DisplayName("All published employee events appear in ascending order")
    @Test
    void givenPublishedEmployeeEventsForAnyEmployee_whenFindAll_thenReturnDescendingOrderedList()
    {
        // Arrange
        String uuid = UUID.randomUUID()
                          .toString();
        receiveRandomMessageFor(uuid);

        // Act
        List<PersistentEmployeeEvent> allDescOrderedById = employeeEventService.findAllByOrderByIdAsc(uuid);

        // Assert
        long previousId = Long.MIN_VALUE;
        for (PersistentEmployeeEvent event : allDescOrderedById)
        {
            long currentId = event.getId();
            assertThat(previousId).isLessThan(currentId);
            previousId = currentId;
        }
    }

    @DisplayName("Handling an employee events makes it persistent")
    @Test
    void givenEmployeeEvent_whenHandle_thenPersistEvent()
    {
        // Arrange
        employeeEventRepository.deleteAll();
        Employee employee = employeeTestFactory.createDefault();
        EmployeeEvent employeeEvent = employeeEventTestFactory.builder()
                                                              .employee(employee)
                                                              .create();

        // Act
        employeeEventService.handleEmployeeEvent(employeeEvent);

        // Assert
        List<PersistentEmployeeEvent> allEvents = employeeEventRepository.findAll();
        assertThat(allEvents).isNotEmpty()
                             .hasSize(1);
        PersistentEmployeeEvent persistentEmployeeEvent = allEvents.get(0);
        assertThat(persistentEmployeeEvent.getId()).isGreaterThan(0);
        assertThat(persistentEmployeeEvent.getBirthday()).isEqualTo(employee.getBirthday());
        assertThat(persistentEmployeeEvent.getDepartmentName()).isEqualTo(employee.getDepartment()
                                                                                  .getDepartmentName());
        assertThat(persistentEmployeeEvent.getEmailAddress()).isEqualTo(employee.getEmailAddress());
        assertThat(persistentEmployeeEvent.getUuid()).isEqualTo(employee.getUuid());
        assertThat(persistentEmployeeEvent.getFirstName()).isEqualTo(employee.getFullName()
                                                                             .getFirstName());
        assertThat(persistentEmployeeEvent.getLastName()).isEqualTo(employee.getFullName()
                                                                            .getLastName());
    }
}