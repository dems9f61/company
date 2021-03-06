package com.takeaway.authorization.user.boundary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.takeaway.authorization.IntegrationTestSuite;
import com.takeaway.authorization.permission.control.PermissionService;
import com.takeaway.authorization.permission.entity.Permission;
import com.takeaway.authorization.role.control.RoleService;
import com.takeaway.authorization.role.entity.Role;
import com.takeaway.authorization.rolepermission.control.RolePermissionService;
import com.takeaway.authorization.runtime.auditing.entity.AuditTrail;
import com.takeaway.authorization.runtime.rest.DataView;
import com.takeaway.authorization.runtime.security.boundary.AccessTokenParameter;
import com.takeaway.authorization.user.entity.User;
import com.takeaway.authorization.user.entity.UserTestFactory;
import com.takeaway.authorization.userrole.control.UserRoleService;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User: StMinko Date: 21.10.2019 Time: 12:13
 *
 * <p>
 */
class UserControllerIntegrationTest extends IntegrationTestSuite
{
    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserRoleService userRoleService;

    @Nested
    @DisplayName("when access")
    class WhenAccess
    {
        @Test
        @DisplayName("GET: 'http://.../users' returns UNAUTHORIZED for missing Authorization header  ")
        void givenMissingAuthorizationHeader_whenFindAll_thenStatus401() throws Exception
        {
            // Arrange
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET: 'http://.../users' returns FORBIDDEN for missing scope  ")
        void givenMissingScope_whenFindAll_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").scopes("bad_scope").build();
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter)).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../users' returns FORBIDDEN for missing role  ")
        void givenMissingRole_whenFindAll_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter)).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../users' returns OK and an list of all users ")
        void givenUsers_whenFindAll_thenStatus200AndReturnFullList() throws Exception
        {
            // Arrange
            List<User> savedUsers = saveRandomUsers(4);
            String uri = String.format("%s?page=%d&size=%d", UserController.BASE_URI, 0, 20);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse()
                                              .getContentAsString();
            Page<User> responsePage = objectMapper.readValue(contentAsString, new TypeReference<Page<User>>() {});
            assertThat(responsePage).isNotNull();
            assertThat(responsePage.getTotalElements()).isEqualTo(savedUsers.size());
            assertThat(savedUsers.stream()
                                        .allMatch(savedUser -> responsePage.stream()
                                                        .anyMatch(role -> role.getId() != null && role.getId().equals(savedUser.getId()))))
                    .isTrue();
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}' returns NOT FOUND for unknown id ")
        void givenUnknownId_whenFindById_thenStatus404() throws Exception
        {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, unknownId.toString()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$", containsString(String.format("Could not find [User] for Id [%s]!", unknownId))));
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}' returns OK and the requested user")
        void givenUser_whenFindById_thenStatus200AndReturnUser() throws Exception
        {
            // Arrange
            User persistedUser = saveRandomUsers(1).get(0);
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(get(uri, persistedUser.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String contentAsString = mvcResult.getResponse().getContentAsString();
            User user = objectMapper.readValue(contentAsString, User.class);
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(persistedUser.getId());
            assertThat(user.getUserName()).isEqualTo(persistedUser.getUserName());
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}/permissions' returns OK and all requested permissions")
        void givePermissions_whenFindByUserId_thenStatus200AndReturnPermissions() throws Exception
        {
            // Arrange
            User persistedUser = saveRandomUsers(1).get(0);
            List<Role> persistedRoles = saveRandomRoles(2);
            Role firstPersistedRole = persistedRoles.get(0);
            List<Permission> expectedPermissionsForFirstRole = saveRandomPermissions(10);
            for (Permission expectedPermission : expectedPermissionsForFirstRole)
            {
                rolePermissionService.assign(firstPersistedRole.getId(), expectedPermission.getId());
            }
            Role secondPersistedRole = persistedRoles.get(1);
            List<Permission> expectedPermissionsForSecondRole = saveRandomPermissions(10);
            for (Permission expectedPermission : expectedPermissionsForSecondRole)
            {
                rolePermissionService.assign(secondPersistedRole.getId(), expectedPermission.getId());
            }
            userRoleService.assign(persistedUser.getId(), firstPersistedRole.getId());
            userRoleService.assign(persistedUser.getId(), secondPersistedRole.getId());
            String uri = String.format("%s/{id}/permissions", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, persistedUser.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$.numberOfElements", is(expectedPermissionsForFirstRole.size() + expectedPermissionsForSecondRole.size())));
        }
    }

    @Nested
    @DisplayName("when update")
    class WhenUpdate
    {
        @Test
        @DisplayName("PUT: 'http://.../users' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenDoFullUpdate_thenStatus401() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.createDefault();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PUT: 'http://.../users' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenDoFullUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.createDefault();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act / Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT: 'http://.../users' returns FORBIDDEN for missing role")
        void givenMissingRole_whenDoFullUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.createDefault();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PUT.class);

            // Act/ Assert
            mockMvc.perform(put(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT: 'http://.../users/{id}' returns OK on valid full request")
        void givenValidFullRequest_whenDoFullUpdate_thenStatus200AndReturnUpdated() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.createDefault();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
            User updated = objectMapper.readValue(contentAsString, User.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getUserName()).isEqualTo(update.getUserName());
        }

        @Test
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on null user name")
        void givenNullUserName_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on empty name")
        void givenBlankUserName_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName("   ").create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on empty confirm password")
        void givenBlankConfirmPassword_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().confirmPassword("   ").create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on null confirm password")
        void givenNullConfirmPassword_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on discrepancy between old password and existing password")
        void givenOldPasswordDifferentFromExistingPassword_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().oldPassword(RandomStringUtils.randomAlphabetic(8)).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PUT: 'http://.../users/{id}' returns BAD REQUEST on discrepancy between  password and confirm password")
        void givenPasswordDifferentFromConfirmPassword_whenDoFullUpdate_thenStatus400() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().confirmPassword(RandomStringUtils.randomAlphabetic(8)).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
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
        @DisplayName("PATCH: 'http://.../users' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenDoPartialUpdate_thenStatus401() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName(RandomStringUtils.randomAlphabetic(8)).oldPassword(null).newPassword(null).confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PATCH.class);

            // Act / Assert
            mockMvc.perform(patch(uri, initial.getId()).contentType(APPLICATION_JSON_UTF8).content(requestAsJson)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("PATCH: 'http://.../users' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenDoPartialUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName(RandomStringUtils.randomAlphabetic(8)).oldPassword(null).newPassword(null).confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PATCH.class);

            // Act / Assert
            mockMvc.perform(patch(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATCH: 'http://.../users' returns FORBIDDEN for missing role")
        void givenMissingRole_whenDoPartialUpdate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName(RandomStringUtils.randomAlphabetic(8)).oldPassword(null).newPassword(null).confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PATCH.class);

            // Act/ Assert
            mockMvc.perform(patch(uri, initial.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATCH: 'http://.../users/{id}' returns OK on new user name")
        void givenNewUseName_whenDoPartialUpdate_thenStatus200() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            User update = userTestFactory.builder().userName(RandomStringUtils.randomAlphabetic(8)).oldPassword(null).newPassword(null).confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PATCH.class);

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
            User updated = objectMapper.readValue(contentAsString, User.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getUserName()).isEqualTo(update.getUserName());
        }

        @Test
        @DisplayName("PATCH: 'http://.../users/{id}' returns BAD_REQUEST on already existing new user name")
        void givenAlreadyExistingNewUseName_whenDoPartialUpdate_thenStatus400() throws Exception
        {
            // Arrange
            List<User> persistedUsers = saveRandomUsers(2);
            User first = persistedUsers.get(0);
            User second = persistedUsers.get(1);

            User secondUpdate = userTestFactory.builder().userName(first.getUserName()).oldPassword(null).newPassword(null).confirmPassword(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(secondUpdate, DataView.PATCH.class);

            // Act / Assert
            mockMvc.perform(put(uri, second.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("PATCH: 'http://.../users/{id}' returns OK on new password")
        void givenNewPassword_whenDoPartialUpdate_thenStatus200() throws Exception
        {
            // Arrange
            User initial = saveRandomUsers(1).get(0);
            String newPassword = RandomStringUtils.randomAlphabetic(8);
            User update = userTestFactory.builder().oldPassword(UserTestFactory.PWD).newPassword(newPassword).confirmPassword(newPassword).userName(null).create();
            String uri = String.format("%s/{id}", UserController.BASE_URI);
            String requestAsJson = transformRequestToJSON(update, DataView.PATCH.class);

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
            User updated = objectMapper.readValue(contentAsString, User.class);
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(initial.getId());
            assertThat(updated.getUserName()).isEqualTo(initial.getUserName());
        }
    }

    @Nested
    @DisplayName("when create")
    class WhenCreate
    {
        @Test
        @DisplayName("POST: 'http://.../users' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenCreate_thenStatus401() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).content(requestAsJson)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            User toPersist = userTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns FORBIDDEN for missing role")
        void givenMissingRole_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            User toPersist = userTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8)
                                    .content(requestAsJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns CREATED if the creation request is valid")
        void givenValidCreateRequest_whenCreate_thenStatus201AndReturnNewEntity() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().userName("user").newPassword("user").confirmPassword("user").create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            MvcResult mvcResult = mockMvc.perform(post(uri)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$", notNullValue()))
                            .andExpect(header().string(HttpHeaders.LOCATION, containsString(UserController.BASE_URI)))
                            .andReturn();

            String contentAsString = mvcResult.getResponse().getContentAsString();
            User created = objectMapper.readValue(contentAsString, User.class);
            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull();
            assertThat(created.getUserName()).isEqualTo(toPersist.getUserName());
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the password and  confirm password do not match")
        void givenNoMatchBetweenPasswordAndConfirmPassword_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().newPassword(RandomStringUtils.randomAlphabetic(9)).confirmPassword(RandomStringUtils.randomAlphabetic(9)).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the user name is null")
        void givenNullUserName_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().userName(null).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the user name is empty")
        void givenEmptyUserName_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().userName("").create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the password is null")
        void givenNullPassword_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().newPassword(null).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the confirm password is null")
        void givenNullConfirmPassword_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User toPersist = userTestFactory.builder().confirmPassword(null).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("POST: 'http://.../users' returns BAD_REQUEST if the given user name already exists")
        void givenAlreadyExistingUserName_whenCreate_thenStatus400() throws Exception
        {
            // Arrange
            User persistedUser = saveRandomUsers(1).get(0);
            User toPersist = userTestFactory.builder().userName(persistedUser.getUserName()).create();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(post(uri).contentType(APPLICATION_JSON_UTF8).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).content(requestAsJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", notNullValue()));
        }
    }

    @Nested
    @DisplayName("when delete")
    class WhenDelete
    {
        @Test
        @DisplayName("DELETE: 'http://.../users' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenDelete_thenStatus401() throws Exception
        {
            // Arrange
            User toDelete = saveRandomUsers(1).get(0);
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act/ Assert
            mockMvc.perform(delete(uri, toDelete.getId()).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE: 'http://.../users' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenDelete_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            User toDelete = saveRandomUsers(1).get(0);
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, toDelete.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE: 'http://.../users' returns FORBIDDEN for missing role")
        void givenMissingRole_whenCreate_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            User toDelete = saveRandomUsers(1).get(0);
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, toDelete.getId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE: 'http://.../users/{id}' returns NOT FOUND if the specified id doesn't exist")
        void givenUnknownUuid_whenDelete_thenStatus404() throws Exception
        {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            String uri = String.format("%s/{id}", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(delete(uri, unknownId).header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken()).contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$", containsString(String.format("Could not find [User] for Id [%s]!", unknownId))));
        }

        @Test
        @DisplayName("DELETE: 'http://.../users/{id}' returns NO CONTENT if the specified id exists")
        void givenUser_whenDelete_thenStatus204() throws Exception
        {
            // Arrange
            User toDelete = saveRandomUsers(1).get(0);
            String uri = String.format("%s/{id}", UserController.BASE_URI);

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
        @DisplayName("GET: 'http://.../users/{id}/auditTrails' returns UNAUTHORIZED for missing Authorization header")
        void givenMissingAuthorizationHeader_whenFindAuditTrails_thenStatus401() throws Exception
        {
            // Arrange
            String uri = String.format("%s/{id}/auditTrails", UserController.BASE_URI);

            // Act/ Assert
            mockMvc.perform(get(uri, UUID.randomUUID()).contentType(APPLICATION_JSON_UTF8)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}/auditTrails' returns FORBIDDEN for missing scope")
        void givenMissingScope_whenFindAuditTrails_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().clientId("clientWithBadScope").clientSecret("secret").build();
            String uri = String.format("%s/{id}/auditTrails", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, UUID.randomUUID())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}/auditTrails' returns FORBIDDEN for missing role")
        void givenMissingRole_whenFindAuditTrails_thenStatus403() throws Exception
        {
            // Arrange
            AccessTokenParameter accessTokenParameter = AccessTokenParameter.builder().userName("userWithNoRole").userPassword("user").build();
            String uri = String.format("%s/{id}/auditTrails", UserController.BASE_URI);

            // Act / Assert
            mockMvc.perform(get(uri, UUID.randomUUID())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken(accessTokenParameter))
                                    .contentType(APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET: 'http://.../users/{id}/auditTrails' returns OK and audit trails")
        void givenIdWithHistory_whenFindAuditTrails_thenStatus200() throws Exception
        {
            // Arrange
            String accessToken = obtainAccessToken();
            // 1-Action: CREATE
            User toPersist = userTestFactory.createDefault();
            String toPersistAstAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            String uri = String.format("%s", UserController.BASE_URI);
            MvcResult mvcCreateResult = mockMvc.perform(post(uri)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(toPersistAstAsJson))
                            .andReturn();
            String createResponseContentAsString = mvcCreateResult.getResponse().getContentAsString();
            User created = objectMapper.readValue(createResponseContentAsString, User.class);

            // 2-Action: UPDATE
            User update = userTestFactory.createDefault();
            uri = String.format("%s/{id}", UserController.BASE_URI);
            String updateAsJson = transformRequestToJSON(update, DataView.PUT.class);
            MvcResult mvcResult = mockMvc.perform(put(uri, created.getId())
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(updateAsJson))
                            .andReturn();
            String updateContentAsString = mvcResult.getResponse().getContentAsString();
            User updated = objectMapper.readValue(updateContentAsString, User.class);

            // 3-Action: DELETE
            mockMvc.perform(delete(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(APPLICATION_JSON_UTF8));

            uri = String.format("%s/{id}/auditTrails", UserController.BASE_URI);

            // Act / Assert
            MvcResult revisionResult = mockMvc.perform(get(uri, created.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(APPLICATION_JSON_UTF8))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", notNullValue()))
                            .andReturn();
            String revisionResultAsString = revisionResult.getResponse()
                                                          .getContentAsString();
            Page<AuditTrail<UUID, User>> responsePage = objectMapper.readValue(revisionResultAsString,
                                                                               new TypeReference<Page<AuditTrail<UUID, User>>>() {});
            assertThat(responsePage).isNotNull().hasSize(3);
            responsePage.forEach(page -> {
                        RevisionType revisionType = page.getRevisionType();
                        User entity = page.getEntity();
                        switch (revisionType)
                        {
                            case ADD:
                                {
                                    assertThat(entity.getId()).isEqualTo(created.getId());
                                    assertThat(entity.getUserName()).isEqualTo(created.getUserName());
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
                                    assertThat(entity.getUserName()).isEqualTo(updated.getUserName());
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

    private List<User> saveRandomUsers(int count) throws Exception
    {
        int normalizedCount = count <= 0 ? 1 : count;
        List<User> result = new ArrayList<>(count);
        String uri = String.format("%s", UserController.BASE_URI);
        for (int i = 0; i < normalizedCount; i++)
        {
            User toPersist = userTestFactory.createDefault();
            String requestAsJson = transformRequestToJSON(toPersist, DataView.POST.class);
            MvcResult mvcResult = mockMvc.perform(post(uri)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtainAccessToken())
                                            .contentType(APPLICATION_JSON_UTF8)
                                            .content(requestAsJson))
                            .andReturn();
            String contentAsString = mvcResult.getResponse().getContentAsString();
            result.add(objectMapper.readValue(contentAsString, User.class));
        }

        return result;
    }

    private List<Role> saveRandomRoles(int count)
    {
        int normalizedCount = count <= 0 ? 1 : count;
        List<Role> result = new ArrayList<>(count);
        for (int i = 0; i < normalizedCount; i++)
        {
            result.add(roleService.create(roleTestFactory.createDefault()));
        }

        return result;
    }

    private List<Permission> saveRandomPermissions(int count)
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
