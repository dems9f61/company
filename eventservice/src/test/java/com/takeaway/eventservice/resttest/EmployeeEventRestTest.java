package com.takeaway.eventservice.resttest;

import com.takeaway.eventservice.RestTestSuite;
import com.takeaway.eventservice.employeeevent.boundary.EmployeeEventController;
import com.takeaway.eventservice.employeeevent.boundary.dto.ApiResponsePage;
import com.takeaway.eventservice.employeeevent.boundary.dto.EmployeeEventResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: StMinko
 * Date: 20.03.2019
 * Time: 21:14
 * <p/>
 */
@DisplayName("Rest tests for the employee event service")
class EmployeeEventRestTest extends RestTestSuite
{
    @Nested
    @DisplayName("when access")
    class WhenAccess
    {
        @Test
        @DisplayName("GET: 'http://.../events/{uuid}' returns OK and an asc sorted list ")
        void givenEmployeeVents_whenFindByUuid_thenStatus200AndAscSortedList()
        {
            // Arrange
            String uuid = UUID.randomUUID()
                              .toString();
            receiveRandomMessageFor(uuid);

            // Act
            ResponseEntity<ApiResponsePage<EmployeeEventResponse>> responseEntity = testRestTemplate.exchange(String.format("%s/%s",
                                                                                                                            EmployeeEventController.BASE_URI,
                                                                                                                            uuid),
                                                                                                              HttpMethod.GET,
                                                                                                              new HttpEntity<>(defaultHttpHeaders()),
                                                                                                              new ParameterizedTypeReference<ApiResponsePage<EmployeeEventResponse>>() {});

            // Assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Page<EmployeeEventResponse> eventResponsePage = responseEntity.getBody();
            assertThat(eventResponsePage).isNotNull()
                                         .isNotEmpty();
            Instant previous = null;
            for (EmployeeEventResponse event : eventResponsePage)
            {
                Instant current = event.getCreatedAt();
                if (previous != null)
                {
                    assertThat(previous).isBefore(current);
                }
                previous = current;
            }
        }
    }
}
