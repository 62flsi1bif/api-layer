/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OIDCExternalMapperTest {

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TokenCreationService tokenCreationService;

    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    private OIDCAuthSource authSource;
    private OIDCExternalMapper oidcExternalMapper;

    private BasicHttpEntity responseEntity;

    private static final String ZOSUSER = "ZOSUSER";
    private static final String SUCCESS_MAPPER_RESPONSE = "{" +
        "\"userid\": \"" + ZOSUSER + "\", " +
        "\"returnCode\": 0, " +
        "\"safReturnCode\": 0, " +
        "\"racfReturnCode\": 0, " +
        "\"racfReasonCode\": 0 " +
        "}";

    private static final String FAILURE_MAPPER_RESPONSE = "{" +
        "\"userid\": \"\", " +
        "\"returnCode\": 8, " +
        "\"safReturnCode\": 8, " +
        "\"racfReturnCode\": 8, " +
        "\"racfReasonCode\": 48 " +
        "}";
    @BeforeEach
    void setup() {
        authSource = new OIDCAuthSource("OIDC_access_token");
        authSource.setDistributedId("distributed_ID");
        oidcExternalMapper = new OIDCExternalMapper("https://domain.com/mapper", "mapper_user", httpClient, tokenCreationService, authConfigurationProperties);
        oidcExternalMapper.registry = "test_registry";

        responseEntity = new BasicHttpEntity();
        responseEntity.setContent(IOUtils.toInputStream(SUCCESS_MAPPER_RESPONSE, StandardCharsets.UTF_8));


    }

    @Nested
    class GivenIdentityMappingExists {

        @BeforeEach
        void setup() throws IOException {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            StatusLine responseStatusLine = mock(StatusLine.class);
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
        }

        @Test
        void thenZosUserIsReturned() throws Exception {
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertEquals(ZOSUSER, userId);
        }

    }

    @Nested
    class GivenNoIdentityMappingExists {

        @BeforeEach
        void setup() throws IOException {
            responseEntity.setContent(IOUtils.toInputStream(FAILURE_MAPPER_RESPONSE, StandardCharsets.UTF_8));

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            StatusLine responseStatusLine = mock(StatusLine.class);
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
        }

        @Test
        void thenNullIsReturned() {
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
        }
    }

    @Nested
    class GivenConfigurationErrors {

        @Test
        void whenAnotherAuthSourceUsed_thenNullIsReturned() throws IOException {
            JwtAuthSource jwtAuthSource = new JwtAuthSource("source");
            String userId = oidcExternalMapper.mapToMainframeUserId(jwtAuthSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any());
        }

        @Test
        void whenRegistryIsNotProvided_thenNullIsReturned() throws IOException {
            oidcExternalMapper.registry = "";
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any());
        }

        @Test
        void whenNoDistributedIdProvided_thenNullIsReturned() throws IOException {
            authSource.setDistributedId("");
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any());
        }

    }

    @Nested
    class GivenErrorsInRequest {
        @Mock
        private ObjectMapper mockedMapper;

        @BeforeEach
        void setup() throws ReflectiveOperationException {
            setFinalStaticField(ExternalMapper.class, "objectMapper", mockedMapper);
        }

        @AfterEach
        void teardown() throws ReflectiveOperationException {
            setFinalStaticField(ExternalMapper.class, "objectMapper", new ObjectMapper());
        }

        @Test
        void whenJsonProcessingException_thenNullIsReturned() throws IOException {
            doThrow(JsonProcessingException.class).when(mockedMapper).writeValueAsString(any());
            String userId = oidcExternalMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(httpClient, times(0)).execute(any());
        }
    }
    private static void setFinalStaticField(Class<?> clazz, String fieldName, Object value)
        throws ReflectiveOperationException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, value);
    }

}
