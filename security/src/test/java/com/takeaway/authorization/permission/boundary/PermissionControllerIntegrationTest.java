package com.takeaway.authorization.permission.boundary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.takeaway.authorization.IntegrationTestSuite;
import com.takeaway.authorization.permission.control.PermissionService;
import com.takeaway.authorization.permission.entity.Permission;
import com.takeaway.authorization.runtime.auditing.entity.AuditTrail;
import com.takeaway.authorization.runtime.rest.DataView;
import com.takeaway.authorization.runtime.security.boundary.AccessTokenParameter;
import org.apache.commons.lang3.RandomUtils;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User: StMinko Date: 17.10.2019 Time: 12:08
 *
 * <p>
 */
@AutoConfigureMockMvc
class PermissionControllerIntegrationTest extends IntegrationTestSuite
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionService permissionService;

    @Nested
    @DisplayName("when access")
    class WhenAccess
    {
        @Test
        @DisplayName("GET: 'http://.../permissions' returns UNAUTHORIZED for missing Authorization header  ")
        void givenMissingAuthorizationHeader_whenFindAll_thenStatus401() throws Exception
        {
            // Arrange
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions' returns FORBIDDEN for missing scope  ")
        void givenMissingScope_whenFindAll_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").scopes("bad_scope").build();
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter)).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions' returns FORBIDDEN for missing role  ")
        void givenMissingRole_whenFindAll_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter)).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions' returns OK and an list of all permissions ")
        void givenPermissions_whenFindAll_thenStatus200AndReturnFullList() throws Exception
        {
            // Arrange
            List<Permission> savedPermissions = saveRandomPermissions(RandomUtils.nextInt(1, 4));
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse()
                                              .getContentAsString();
            Page<Permission> responsePage = objectMapper.readValue(contentAsString, new TypeReference<Page<Permission>>() {});

            assertThat(responsePage).isNotNull();
            assertThat(responsePage.getTotalElements()).isEqualTo(savedPermissions.size());
            assertThat(savedPermissions.stream()
                                        .allMatch(savedPermission -> responsePage.stream()
                                                        .anyMatch(permission -> permission.getId() != null && permission.getId().equals(savedPermission.getId()))))
                    .isTrue();
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}' returns NOT FOUND for unknown id ")
        void givenUnknownId_whenFindById_thenStatus404() throws Exception
        {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, unknownId).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$", containsString(String.format("Could not find [Permission] for Id [%s]!", unknownId))));
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}' returns OK and the requested permission")
        void givenPermission_whenFindById_thenStatus200AndReturnPermission() throws Exception
        {
            // Arrange
            Permission savedPermission = saveRandomPermissions(1).get(0);
            UUID id = savedPermission.getId();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, id).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$.id", is(id.toString())))
                    .andExpect(jsonPath("$.name", is(savedPermission.getName())))
                    .andExpect(jsonPath("$.description", is(savedPermission.getDescription())))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.createdBy", notNullValue()))
                    .andExpect(jsonPath("$.lastUpdatedAt", notNullValue()))
                    .andExpect(jsonPath("$.lastUpdatedBy", notNullValue()));
        }
    }

    @Nested
    @DisplayName("when create")
    class WhenCreate
    {
        @Test
        @DisplayName("POST: 'http://.../permissions' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenCreate_thenStatus401() throws Exception
        {
            // Arrange
            Permission toPersist = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).content(requestAsJson)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            Permission toPersist = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns FORBIDDEN for missing role")
        void givenMissingRole_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            Permission toPersist = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns CREATED if the creation request is valid")
        void givenValidCreateRequest_whenCreatePermission_thenStatus201AndReturnNewPermission() throws Exception
        {
            // Arrange
            Permission toPersist = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, notNullValue()))
                    .andExpect(header().string(HttpHeaders.LOCATION, containsString(PermissionController.BASE_URI)))
                    .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is(toPersist.getName())))
                    .andExpect(jsonPath("$.description", is(toPersist.getDescription())))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.createdBy", notNullValue()))
                    .andExpect(jsonPath("$.lastUpdatedAt", notNullValue()))
                    .andExpect(jsonPath("$.version", is(0)))
                    .andExpect(jsonPath("$.lastUpdatedBy", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns CREATED if the creation request without description")
        void givenCreateRequestWithoutDesc_whenCreatePermission_thenStatus201AndReturnNewPermission() throws Exception
        {
            // Arrange
            Permission toPersist = permissionTestFactory.builder().description(null).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);

            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, containsString(PermissionController.BASE_URI)))
                    .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is(toPersist.getName())))
                    .andExpect(jsonPath("$.description", nullValue()))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.createdBy", notNullValue()))
                    .andExpect(jsonPath("$.lastUpdatedAt", notNullValue()))
                    .andExpect(jsonPath("$.lastUpdatedBy", notNullValue()))
                    .andExpect(jsonPath("$.version", is(0)));
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns BAD_REQUEST on blank identifier")
        void givenBlankName_whenCreatePermission_thenStatus400() throws Exception
        {
            // Arrange
            Permission toPersist = permissionTestFactory.builder().name("").create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../permissions' returns BAD_REQUEST on blank description")
        void givenBlankDescription_whenCreatePermission_thenStatus400() throws Exception
        {
            // Arrange
            Permission toPersist = permissionTestFactory.builder().description("").create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }
    }

    @Nested
    @DisplayName("when delete")
    class WhenDelete
    {
        @Test
        @DisplayName("DELETE: 'http://.../permissions' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenDelete_thenStatus401() throws Exception
        {
            // Arrange
            Permission toDelete = saveRandomPermissions(1).get(0);
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act/ Assert
            mockMvc.perform(delete(uri, toDelete.getId()).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE: 'http://.../permissions' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenDelete_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            Permission toDelete = saveRandomPermissions(1).get(0);
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, toDelete.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE: 'http://.../permissions' returns FORBIDDEN for missing role")
        void givenMissingRole_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            Permission toDelete = saveRandomPermissions(1).get(0);
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, toDelete.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE: 'http://.../permissions/{id}' returns NOT FOUND if the specified id doesn't exist")
        void givenUnknownUuid_whenDeleteById_thenStatus404() throws Exception
        {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act/ Assert
            mockMvc.perform(delete(uri, unknownId).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$", containsString(String.format("Could not find [Permission] for Id [%s]!", unknownId))));
        }

        @Test
        @DisplayName("DELETE: 'http://.../permissions/{id}' returns NO CONTENT if the specified id exists")
        void givenPermission_whenDeleteById_thenStatus204() throws Exception
        {
            // Arrange
            Permission toDelete = saveRandomPermissions(1).get(0);
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, toDelete.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("when revise")
    class WhenRevise
    {
        @Test
        @DisplayName("GET: 'http://.../permissions/{id}/auditTrails' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenFindAuditTrails_thenStatus401() throws Exception
        {
            // Arrange
            String uri = String.format("%s/{id}/auditTrails", PermissionController.BASE_URI);

            // Act/ Assert
            mockMvc.perform(get(uri, UUID.randomUUID()).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}/auditTrails' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenFindAuditTrails_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            String uri = String.format("%s/{id}/auditTrails", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, UUID.randomUUID())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}/auditTrails' returns FORBIDDEN for missing role")
        void givenMissingRole_whenFindAuditTrails_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            String uri = String.format("%s/{id}/auditTrails", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, UUID.randomUUID())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}/revisions' returns OK and Revisions")
        void givenIdWithHistory_whenFindRevisions_thenStatus200() throws Exception
        {
            // Arrange
            Permission initial = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(initial, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // 1-Action: CREATE
            MvcResult mvcCreationResult = mockMvc.perform(post(uri)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andReturn();
            String createdContentAsString = mvcCreationResult.getResponse().getContentAsString();
            Permission created = objectMapper.readValue(createdContentAsString, Permission.class);
            Permission update = permissionTestFactory.createDefault();
            uri = String.format("%s/{id}", PermissionController.BASE_URI);
            requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // 2-Action: MODIFY
            mockMvc.perform(put(uri, created.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(requestAsJson));

            // 3-Action: DELETE
            mockMvc.perform(delete(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8));

            uri = String.format("%s/{id}/revisions", PermissionController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$.numberOfElements", is(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("GET: 'http://.../permissions/{id}/auditTrails' returns OK and Revisions")
        void givenIdWithHistory_whenFindAuditTrails_thenStatus200() throws Exception
        {
            // Arrange
            Permission initial = permissionTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(initial, DataView.POST.class);
            String uri = String.format("%s", PermissionController.BASE_URI);

            // 1-Action: CREATE
            MvcResult mvcCreationResult = mockMvc.perform(post(uri)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andReturn();
            String createdContentAsString = mvcCreationResult.getResponse().getContentAsString();
            Permission created = objectMapper.readValue(createdContentAsString, Permission.class);
            Permission update = permissionTestFactory.createDefault();
            uri = String.format("%s/{id}", PermissionController.BASE_URI);
            requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // 2-Action: MODIFY
            MvcResult mvcResult = mockMvc.perform(put(uri, created.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andReturn();
            String updatedContentAsString = mvcResult.getResponse().getContentAsString();
            Permission updated = objectMapper.readValue(updatedContentAsString, Permission.class);

            // 3-Action: DELETE
            mockMvc.perform(delete(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8));

            uri = String.format("%s/{id}/auditTrails", PermissionController.BASE_URI);

            // Act / Assert
            MvcResult revisionResult = mockMvc.perform(get(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                                                                .contentType(APPLICATION_JSON_UTF8))
                                              .andExpect(status().isOk())
                                              .andExpect(jsonPath("$", notNullValue()))
                                              .andReturn();
            String revisionResultAsString = revisionResult.getResponse()
                                                          .getContentAsString();
            Page<AuditTrail<UUID, Permission>> responsePage = objectMapper.readValue(revisionResultAsString,
                                                                                     new TypeReference<Page<AuditTrail<UUID, Permission>>>() {});
            assertThat(responsePage).isNotNull()
                                    .hasSize(3);

            responsePage.forEach(page -> {
                RevisionType revisionType = page.getRevisionType();
                Permission entity = page.getEntity();
                switch (revisionType)
                {
                    case ADD:
                    {
                        assertThat(entity.getId()).isEqualTo(created.getId());
                                    assertThat(entity.getName()).isEqualTo(created.getName());
                                    assertThat(entity.getDescription()).isEqualTo(created.getDescription());
                                    assertThat(entity.getLastUpdatedAt()).isEqualTo(created.getLastUpdatedAt());
                                    assertThat(entity.getLastUpdatedBy()).isEqualTo(created.getLastUpdatedBy());
                                    assertThat(entity.getCreatedAt()).isNull(); // NOT AUDITED
                                    assertThat(entity.getCreatedBy()).isNull(); // NOT AUDITED
                                    assertThat(entity.getVersion()).isEqualTo(created.getVersion());
                                    break;
                                }
                            case MOD:
                            case DEL:
                                {
                                    assertThat(entity.getId()).isEqualTo(updated.getId());
                                    assertThat(entity.getName()).isEqualTo(updated.getName());
                                    assertThat(entity.getDescription()).isEqualTo(updated.getDescription());
                                    assertThat(entity.getLastUpdatedAt()).isEqualTo(updated.getLastUpdatedAt());
                                    assertThat(entity.getLastUpdatedBy()).isEqualTo(updated.getLastUpdatedBy());
                                    assertThat(entity.getCreatedAt()).isNull(); // NOT AUDITED
                                    assertThat(entity.getCreatedBy()).isNull(); // NOT AUDITED
                                    assertThat(entity.getVersion()).isEqualTo(updated.getVersion());
                                    break;
                                }
                            default:
                                {
                                    fail(String.format("Should not happen: Unexpected revision type [%s]!", revisionType));
                                }
                        }
                    });
        }
    }

    @Nested
    @DisplayName("when update")
    class WhenUpdate
    {
        @Test
        @DisplayName("PUT: 'http://.../permissions' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenDoFullUpdate_thenStatus401() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.createDefault();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenDoFullUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.createDefault();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions' returns FORBIDDEN for missing role")
        void givenMissingRole_whenDoFullUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.createDefault();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act/ Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions/{id}' returns OK on valid full request")
        void givenValidFullRequest_whenDoFullUpdate_thenStatus200AndReturnUpdated() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.createDefault();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(put(uri, initial.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse().getContentAsString();
            Permission updated = objectMapper.readValue(contentAsString, Permission.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getName()).isEqualTo(update.getName());
            assertThat(updated.getDescription()).isEqualTo(update.getDescription());
            assertThat(updated.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions/{id}' returns BAD REQUEST on null name")
        void givenNullName_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().name(null).create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions/{id}' returns BAD REQUEST on empty name")
        void givenBlankName_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().name("   ").create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PUT: 'http://.../permissions/{id}' returns BAD REQUEST on null description request")
        void givenNullDescription_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().description(null).create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PATCH: 'http://.../permissions/{id}' returns OK on valid request with only name")
        void givenValidRequestWithOnlyName_whenDoPartialUpdate_thenStatus200AndReturnUpdated() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().description(null).create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(patch(uri, initial.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse().getContentAsString();
            Permission updated = objectMapper.readValue(contentAsString, Permission.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getName()).isEqualTo(update.getName());
            assertThat(updated.getDescription()).isEqualTo(initial.getDescription());
        }

        @Test
        @DisplayName("PATCH: 'http://.../permissions/{id}' returns OK on valid request with only description")
        void givenValidRequestWithOnlyDesc_whenDoPartialUpdate_thenStatus200AndReturnUpdated() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().name(null).create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(patch(uri, initial.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse().getContentAsString();
            Permission updated = objectMapper.readValue(contentAsString, Permission.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getName()).isEqualTo(initial.getName());
            assertThat(updated.getDescription()).isEqualTo(update.getDescription());
        }

        @Test
        @DisplayName("PATCH: 'http://.../permissions/{id}' returns BAD REQUEST on empty description request")
        void givenEmptyDescription_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().description(" ").create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PATCH: 'http://.../permissions/{id}' returns BAD REQUEST on empty description request")
        void givenEmptyDescription_whenDoPartialUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().description(" ").create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(patch(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PATCH: 'http://.../permissions/{id}' returns BAD REQUEST on empty name request")
        void givenEmptyName_whenDoPartialUpdate_thenStatus400() throws Exception
        {
            // Arrange
            Permission initial = saveRandomPermissions(1).get(0);
            Permission update = permissionTestFactory.builder().name(" ").create();
            String uri = String.format("%s/{id}", PermissionController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(patch(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }
    }

    private List<Permission> saveRandomPermissions(int count) throws Exception
    {
        int normalizedCount = count <= 0 ? 1 : count;
        List<Permission> result = new ArrayList<>(count);
        for (int i = 0; i < normalizedCount; i++)
        {
            result.add(permissionService.create(permissionTestFactory.createDefault()));
        }

        return result;
    }
}
