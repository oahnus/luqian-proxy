package com.github.oahnus.proxyserver.config.security;

import com.github.oahnus.proxyserver.entity.SysPermission;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.SysPermService;
import com.github.oahnus.proxyserver.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oahnus on 2020-04-26
 * 18:25.
 */
@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private SysUserService userService;
    @Autowired
    private SysPermService permService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET,
                        "/",
                        "/*.html",
                        "/*.css",
                        "/favicon.ico",
                        "/*.js")
                .permitAll()
                .antMatchers(HttpMethod.OPTIONS)//跨域请求会先进行一次options请求
                .permitAll()
                .antMatchers(
                        "/login",
                        "/register",
                        "/open/**",
                        "/version/**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(new GoAccessDeniedHandler())
                .authenticationEntryPoint(new GoAuthEntryPoint())
                .and()
                .formLogin()
                .loginPage("/login")
                .successHandler(new GoAuthSuccessHandler())
                .failureHandler(new GoAuthFailureHandler())
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new GoLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .and()
                .addFilterBefore(new WebSecurityCorsFilter(), ChannelProcessingFilter.class)
                .addFilter(new JWTAuthFilter(authenticationManager(), userDetailsService()))
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        super.configure(http);
    }

    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
                if (StringUtils.isEmpty(s)) {
                    throw new DisabledException("用户名不能为空");
                }
                SysUser sysUser = userService.getUserByUsername(s);
                if (sysUser == null) {
                    throw new DisabledException("用户名或密码错误");
                }

                Long userId = sysUser.getId();

                List<SysPermission> permissions = permService.listByUserId(userId);
                List<SimpleGrantedAuthority> authorities = permissions.stream().map(perm -> {
                    String value = perm.getValue();
                    return new SimpleGrantedAuthority(value);
                }).collect(Collectors.toList());

                SysUserDetails userDetails = new SysUserDetails(sysUser);
                userDetails.setAuthorities(authorities);
                return userDetails;
            }
        };
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.eraseCredentials(false);
        auth.userDetailsService(userDetailsService());
        auth.authenticationProvider(new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String name = authentication.getName();
                String password = (String) authentication.getCredentials();

                UserDetails userDetails = userDetailsService().loadUserByUsername(name);

                boolean matches = passwordEncoder().matches(password, userDetails.getPassword());
                if (!matches) {
                    throw new DisabledException("用户名或密码错误");
                }

                return new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
            }

            @Override
            public boolean supports(Class<?> aClass) {
                return true;
            }
        });
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
