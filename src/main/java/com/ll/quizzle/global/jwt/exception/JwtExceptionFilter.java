package com.ll.quizzle.global.jwt.exception;

import com.ll.quizzle.global.exceptions.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Order(0)
public class JwtExceptionFilter extends OncePerRequestFilter {

/*   filter 는 DispatcherServlet 앞단에서 동작하기 때문에, filter 에서 발생한 예외는
     DispatcherServlet 까지 전달되지 않아서 GlobalExceptionHandler 에서 처리할 수 없음 */

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ServiceException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(String.format(
                    "{\"resultCode\": \"%d-1\", \"msg\": \"%s\", \"data\": null}",
                    HttpServletResponse.SC_UNAUTHORIZED,
                    e.getMessage()
            ));
        }
    }
}