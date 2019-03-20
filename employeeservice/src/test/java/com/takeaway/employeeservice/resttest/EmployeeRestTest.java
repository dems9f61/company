package com.takeaway.employeeservice.resttest;

import com.takeaway.employeeservice.ApiVersions;
import com.takeaway.employeeservice.department.api.dto.DepartmentRequest;
import com.takeaway.employeeservice.department.api.dto.DepartmentResponse;
import com.takeaway.employeeservice.employee.api.dto.EmployeeRequest;
import com.takeaway.employeeservice.employee.api.dto.EmployeeResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: StMinko
 * Date: 20.03.2019
 * Time: 00:46
 * <p/>
 */
@DisplayName("Rest tests for the employee service")
class EmployeeRestTest extends RestTestSuite
{
    @Nested
    @DisplayName("when create")
    class WhenCreate
    {
        @Test
        @DisplayName("POST: 'http://.../employees' returns CREATED if the specified parameters are valid")
        void givenValidParameters_whenCreateEmployee_thenStatus201()
        {
            // Arrange
            String departmentName = RandomStringUtils.randomAlphabetic(23);
            createAndPersistDepartment(departmentName);
            EmployeeRequest employeeRequest = employeeRequestTestFactory.builder()
                                                                        .departmentName(departmentName)
                                                                        .create();
            String uri = String.format("%s/employees", ApiVersions.V1);
            HttpHeaders headers = defaultHttpHeaders();

            // Act
            ResponseEntity<EmployeeResponse> responseEntity = testRestTemplate.exchange(uri,
                                                                                        HttpMethod.POST,
                                                                                        new HttpEntity<>(employeeRequest, headers),
                                                                                        EmployeeResponse.class);

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            EmployeeResponse employeeResponse = responseEntity.getBody();
            assertThat(employeeResponse).isNotNull();
            assertThat(employeeResponse.getUuid()).isNotBlank();
            assertThat(employeeResponse.getDepartmentName()).isEqualTo(employeeRequest.getDepartmentName());
            assertThat(employeeResponse.getEmailAddress()).isEqualTo(employeeRequest.getEmailAddress());
            assertThat(employeeResponse.getFirstName()).isEqualTo(employeeRequest.getFirstName());
            assertThat(employeeResponse.getLastName()).isEqualTo(employeeRequest.getLastName());
            assertThat(employeeResponse.getBirthday()).isEqualTo(employeeRequest.getBirthday());
        }

        @Test
        @DisplayName("POST: 'http://.../employees' returns BAD REQUEST if the specified department name doesn't exist ")
        void givenUnknownDepartment_whenCreateEmployee_thenStatus400()
        {
            EmployeeRequest employeeRequest = employeeRequestTestFactory.createDefault();
            String uri = String.format("%s/employees", ApiVersions.V1);
            HttpHeaders headers = defaultHttpHeaders();

            // Act
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(uri,
                                                                              HttpMethod.POST,
                                                                              new HttpEntity<>(employeeRequest, headers),
                                                                              String.class);

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("POST: 'http://.../employees' returns BAD REQUEST if the specified email already exists ")
        void givenAlreadyUsedEmail_whenCreateEmployee_thenStatus400()
        {
            // Arrange
            String departmentName = RandomStringUtils.randomAlphabetic(23);
            createAndPersistDepartment(departmentName);
            EmployeeRequest employeeRequest = employeeRequestTestFactory.builder()
                                                                        .departmentName(departmentName)
                                                                        .create();
            createAndPersistEmployee(employeeRequest);
            String uri = String.format("%s/employees", ApiVersions.V1);
            HttpHeaders headers = defaultHttpHeaders();

            EmployeeRequest newEmployeeRequest = employeeRequestTestFactory.builder()
                                                                           .emailAddress(employeeRequest.getEmailAddress())
                                                                           .create();

            // Act
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(uri,
                                                                              HttpMethod.POST,
                                                                              new HttpEntity<>(newEmployeeRequest, headers),
                                                                              String.class);

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("when access")
    class WhenAccess
    {
        @Test
        @DisplayName("GET: 'http://.../employees/{uuid}' returns NOT FOUND if the specified uuid doesn't exist ")
        void givenUnknownUuid_whenFindEmployeeByUuid_thenStatus404()
        {
            // Arrange
            String uri = String.format("%s/employees", ApiVersions.V1);
            String wrongUuid = UUID.randomUUID()
                                   .toString();

            // Act
            ResponseEntity<Void> responseEntity = testRestTemplate.exchange(String.format("%s/%s", uri, wrongUuid),
                                                                            HttpMethod.GET,
                                                                            new HttpEntity<>(defaultHttpHeaders()),
                                                                            Void.class);

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("GET: 'http://.../employees/{uuid}' returns OK if the specified uuid exists ")
        void givenEmployee_whenFindEmployeeByUuid_thenStatus200()
        {
            // Arrange
            String departmentName = RandomStringUtils.randomAlphabetic(23);
            createAndPersistDepartment(departmentName);
            EmployeeRequest employeeRequest = employeeRequestTestFactory.builder()
                                                                        .departmentName(departmentName)
                                                                        .create();
            EmployeeResponse persistedEmployee = createAndPersistEmployee(employeeRequest);
            String uuidToFind = persistedEmployee.getUuid();
            String uri = String.format("%s/employees", ApiVersions.V1);

            // Act
            ResponseEntity<EmployeeResponse> responseEntity = testRestTemplate.exchange(String.format("%s/%s", uri, uuidToFind),
                                                                                        HttpMethod.GET,
                                                                                        new HttpEntity<>(defaultHttpHeaders()),
                                                                                        EmployeeResponse.class);

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo(persistedEmployee);
        }
    }

    private void createAndPersistDepartment(String departmentName)
    {
        DepartmentRequest createDepartmentRequest = departmentRequestTestFactory.builder()
                                                                                .departmentName(departmentName)
                                                                                .create();
        String uri = String.format("%s/departments", ApiVersions.V1);
        HttpHeaders headers = defaultHttpHeaders();
        testRestTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(createDepartmentRequest, headers), DepartmentResponse.class);
    }

    private EmployeeResponse createAndPersistEmployee(EmployeeRequest employeeRequest)
    {
        String uri = String.format("%s/employees", ApiVersions.V1);
        HttpHeaders headers = defaultHttpHeaders();

        // Act
        return testRestTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(employeeRequest, headers), EmployeeResponse.class)
                               .getBody();
    }
}
