package com.sleekydz86.catalog.unit.security;

import com.sleekydz86.catalog.global.security.SecurityAuthenticatedUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("인증 사용자 식별자 제공자")
class SecurityAuthenticatedUserProviderTest {

    private final SecurityAuthenticatedUserProvider provider = new SecurityAuthenticatedUserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("익명 인증이면 userId 헤더를 사용한다")
    void usesUserIdHeaderWhenAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("userId", "operator-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(provider.currentUserId()).isEqualTo("operator-1");
    }

    @Test
    @DisplayName("인증과 헤더가 없으면 system을 반환한다")
    void defaultsToSystem() {
        assertThat(provider.currentUserId()).isEqualTo("system");
    }
}
