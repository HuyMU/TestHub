package com.testhub.testflowlite.auth;

import com.testhub.testflowlite.common.ApiResponse;
import com.testhub.testflowlite.common.InvalidCredentialsException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.config.JwtConfig;
import com.testhub.testflowlite.security.RefreshCookieFactory;
import com.testhub.testflowlite.user.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerUnitTest {

    private AuthService stubAuthService;
    private RefreshCookieFactory refreshCookieFactory;
    private JwtConfig jwtConfig;
    private AuthController authController;

    private ApiResponse<TokenResponse> stubLoginResult;
    private ApiResponse<TokenResponse> stubRefreshResult;

    @BeforeEach
    void setUp() {
        stubAuthService = new AuthService(null, null, null, null) {
            @Override
            public ApiResponse<TokenResponse> login(LoginRequest request) {
                return stubLoginResult;
            }

            @Override
            public ApiResponse<TokenResponse> refreshToken(String refreshToken) {
                return stubRefreshResult;
            }
        };

        refreshCookieFactory = new RefreshCookieFactory();
        ReflectionTestUtils.setField(refreshCookieFactory, "cookieSecure", false);

        jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "refreshExpirationMs", 604800000L);

        authController = new AuthController(stubAuthService, refreshCookieFactory, jwtConfig);
    }

    @Test
    void testLogin_SetsHttpOnlyCookie_AndNullsRefreshTokenInBody() {
        LoginRequest request = new LoginRequest("tester", "Password@123");
        UserDto userDto = new UserDto(1L, "tester", "tester@test.com", "Tester User", Role.TESTER, true);
        TokenResponse tokenResponse = new TokenResponse("access-token-123", "refresh-token-456", userDto);
        stubLoginResult = ApiResponse.success("Login successful", tokenResponse);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<TokenResponse> result = authController.login(request, response);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("access-token-123", result.getData().getAccessToken());
        assertNull(result.getData().getRefreshToken());

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token=refresh-token-456"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Path=/api/auth"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    void testRefresh_WithCookie_ReissuesCookie_AndReturnsNewAccessToken() {
        UserDto userDto = new UserDto(1L, "tester", "tester@test.com", "Tester User", Role.TESTER, true);
        TokenResponse tokenResponse = new TokenResponse("new-access-token-789", "refresh-token-456", userDto);
        stubRefreshResult = ApiResponse.success("Token refreshed successfully", tokenResponse);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<TokenResponse> result = authController.refreshToken("refresh-token-456", response);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("new-access-token-789", result.getData().getAccessToken());
        assertNull(result.getData().getRefreshToken());

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token=refresh-token-456"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Path=/api/auth"));
    }

    @Test
    void testRefresh_MissingOrBlankCookie_ThrowsInvalidCredentials() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(InvalidCredentialsException.class, () -> authController.refreshToken(null, response));
        assertThrows(InvalidCredentialsException.class, () -> authController.refreshToken("", response));
        assertThrows(InvalidCredentialsException.class, () -> authController.refreshToken("   ", response));
    }

    @Test
    void testLogout_SetsMaxAgeZeroCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> result = authController.logout(response);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Logged out successfully", result.getMessage());

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Path=/api/auth"));
    }

    @Test
    void testRefreshCookieFactory_BuildsCorrectCookieProperties() {
        ResponseCookie cookie = refreshCookieFactory.buildRefreshCookie("sample-token", 3600);

        assertEquals("refresh_token", cookie.getName());
        assertEquals("sample-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge().getSeconds());
        assertEquals("Lax", cookie.getSameSite());
    }
}
