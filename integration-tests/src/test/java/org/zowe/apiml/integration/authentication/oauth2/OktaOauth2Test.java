/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.oauth2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.integration.authentication.oauth2.model.ZssResponse;
import org.zowe.apiml.integration.authentication.pat.ValidateRequestModel;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.zowe.apiml.util.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("OktaOauth2Test")
public class OktaOauth2Test {

    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_OIDC_TOKEN);
    private static final String VALID_TOKEN_WITH_MAPPING = SecurityUtils.validOktaAccessToken(true);
    private static final String VALID_TOKEN_NO_MAPPING = SecurityUtils.validOktaAccessToken(false);
    private static final String EXPIRED_TOKEN = SecurityUtils.expiredOktaAccessToken();

    private static Stream<Arguments> validTokens() {
        return Stream.of(
            Arguments.of(VALID_TOKEN_WITH_MAPPING),
            Arguments.of(VALID_TOKEN_NO_MAPPING)
        );
    }

    private static Stream<Arguments> invalidTokens() {
        return Stream.of(
            Arguments.of(EXPIRED_TOKEN),
            Arguments.of("invalid_token")
        );
    }

    @BeforeAll
    static void init() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class GivenValidOktaToken {
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#validTokens")
        void thenValidateReturns200(String validToken) {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(validToken);
            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(HttpStatus.SC_OK);
        }

        @Nested
        class WhenTestingZoweJwtScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);

            @Test
            void whenUserHasMapping_thenApimlTokenCreated() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", not(hasKey("x-zowe-auth-failure")))
                    .body("headers", hasKey("cookie"))
                    .body("cookies", hasKey("apimlAuthenticationToken"))
                    .body("cookies.apimlAuthenticationToken", not(is(VALID_TOKEN_WITH_MAPPING)));
            }

            @Test
            void whenUserHasNoMapping_thenZoweAuthFailure() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_NO_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("cookie")));
            }

            @Nested
            class WhenTokenInApimlCookie {

                @Test
                void whenUserHasMapping_thenApimlTokenCreated() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_WITH_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", not(hasKey("x-zowe-auth-failure")))
                        .body("headers", hasKey("cookie"))
                        .body("cookies", hasKey("apimlAuthenticationToken"))
                        .body("cookies.apimlAuthenticationToken", not(is(VALID_TOKEN_WITH_MAPPING)));
                }

                @Test
                void whenUserHasNoMapping_thenZoweAuthFailure() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_NO_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", hasKey("x-zowe-auth-failure"))
                        .body("cookies", hasKey("apimlAuthenticationToken"))
                        .body("cookies.apimlAuthenticationToken", is(VALID_TOKEN_NO_MAPPING));
                }
            }
        }

        @Nested
        class WhenTestingZosmfScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(ZOSMF_REQUEST);

            @Test
            void whenUserHasMapping_thenJwtTokenCreated() {
                 given()
                    .contentType(ContentType.JSON)
                     .header(HttpHeaders.AUTHORIZATION,
                         ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", not(hasKey("x-zowe-auth-failure")))
                    .body("headers", hasKey("cookie"))
                    .body("cookies", hasKey("jwtToken"));
            }

            @Test
            void whenUserHasNoMapping_thenZoweAuthFailure() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_NO_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("cookie")));
            }

            @Nested
            class WhenTokenInApimlCookie {

                @Test
                void whenUserHasMapping_thenJwtTokenCreated() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_WITH_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", not(hasKey("x-zowe-auth-failure")))
                        .body("headers", hasKey("cookie"))
                        .body("cookies", hasKey("jwtToken"));
                }

                @Test
                void whenUserHasNoMapping_thenZoweAuthFailure() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_NO_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", hasKey("x-zowe-auth-failure"))
                        .body("headers", hasKey("cookie"))
                        .body("cookies", not(hasKey("jwtToken")));
                }
            }
        }

        @Nested
        class WhenTestingSafIdtScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);

            @Test
            void whenUserHasMapping_thenSafTokenCreated() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", not(hasKey("x-zowe-auth-failure")))
                    .body("headers", hasKey("x-saf-token"));
            }

            @Test
            void whenUserHasNoMapping_thenZoweAuthFailure() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_NO_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("x-saf-token")));
            }

            @Nested
            class WhenTokenInApimlCookie {

                @Test
                void whenUserHasMapping_thenSafTokenCreated() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_WITH_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", not(hasKey("x-zowe-auth-failure")))
                        .body("headers", hasKey("x-saf-token"));
                }

                @Test
                void whenUserHasNoMapping_thenZoweAuthFailure() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_NO_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", hasKey("x-zowe-auth-failure"))
                        .body("headers", not(hasKey("x-saf-token")));
                }
            }
        }

        @Nested
        class WhenTestingPassticketScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);

            @Test
            void whenUserHasMapping_thenBasicAuthCreated() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", not(hasKey("x-zowe-auth-failure")))
                    .body("headers", hasKey("authorization"))
                    .body("headers.authorization", startsWith("Basic"));
            }

            @Test
            void whenUserHasNoMapping_thenZoweAuthFailure() {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_NO_MAPPING)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", hasKey("authorization"))
                    .body("headers.authorization", not(startsWith("Basic")));
            }

            @Nested
            class WhenTokenInApimlCookie {

                @Test
                void whenUserHasMapping_thenBasicAuthCreated() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_WITH_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", not(hasKey("x-zowe-auth-failure")))
                        .body("headers", hasKey("authorization"))
                        .body("headers.authorization", startsWith("Basic"));
                }

                @Test
                void whenUserNoHasMapping_thenZoweAuthFailure() {
                    given()
                        .contentType(ContentType.JSON)
                        .cookie(GATEWAY_TOKEN_COOKIE_NAME, VALID_TOKEN_NO_MAPPING)
                        .when()
                        .get(DC_url)
                        .then().statusCode(200)
                        .body("headers", hasKey("x-zowe-auth-failure"))
                        .body("headers", not(hasKey("authorization")));
                }
            }
        }
    }

    @Nested
    class GivenInvalidOktaToken {

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
        void thenValidateReturns401(String invalidToken) {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(invalidToken);
            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Nested
        class WhenTestingZoweJwtScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInHeader_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("cookie")));
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInCookie_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("cookies", hasKey("apimlAuthenticationToken"))
                    .body("cookies.apimlAuthenticationToken", is(invalidToken));
            }
        }

        @Nested
        class WhenTestingZosmfScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(ZOSMF_REQUEST);

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInHeader_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("cookie")));
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInCookie_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", hasKey("cookie"))
                    .body("cookies", not(hasKey("jwtToken")));
            }
        }

        @Nested
        class WhenTestingSafIdtScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInHeader_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("x-saf-token")));
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInCookie_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("x-saf-token")));
            }
        }

        @Nested
        class WhenTestingPassticketScheme {
            private final URI DC_url = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInHeader_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                        ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", hasKey("authorization"))
                    .body("headers.authorization", not(startsWith("Basic")));
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.oauth2.OktaOauth2Test#invalidTokens")
            void whenTokenInCookie_thenZoweAuthFailure(String invalidToken) {
                given()
                    .contentType(ContentType.JSON)
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, invalidToken)
                    .when()
                    .get(DC_url)
                    .then().statusCode(200)
                    .body("headers", hasKey("x-zowe-auth-failure"))
                    .body("headers", not(hasKey("authorization")));
            }
        }
    }

    @Nested
    class GivenMappingOrZssErrors {

        private final URI DC_url = HttpRequestUtils.getUriFromGateway(ZOSMF_REQUEST);
        @Test
        void testEmptyDistuinguishedNameError() {
            setZssResponse(200, ZssResponse.ZssError.MAPPING_EMPTY_INPUT);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }

        @Test
        void testUserIsNotAuthorizedToQueryMapping() {
            setZssResponse(200, ZssResponse.ZssError.MAPPING_NOT_AUTHORIZED);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }

        @Test
        void testOtherMappingError() {
            setZssResponse(200, ZssResponse.ZssError.MAPPING_OTHER);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }

        @Test
        void testZssReturns401() {
            setZssResponse(401, ZssResponse.ZssError.MAPPING_OTHER);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }
        @Test
        void testZssReturns404() {
            setZssResponse(404, ZssResponse.ZssError.MAPPING_OTHER);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }

        @Test
        void testZssReturns500() {
            setZssResponse(500, ZssResponse.ZssError.MAPPING_OTHER);

            given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION,
                    ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN_WITH_MAPPING)
                .when()
                .get(DC_url)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers", not(hasKey("cookie")));
        }
    }

    private void setZssResponse(int statusCode, ZssResponse.ZssError zssError) {
        final URI mockZssUrl = HttpRequestUtils.getUriFromGateway("/zss/api/v1/certificate/dn/mock-response");
        ZssResponse requestBody = new ZssResponse(statusCode, zssError);
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(mockZssUrl)
            .then().statusCode(HttpStatus.SC_CREATED);
    }
}
