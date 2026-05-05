package com.cliniradar.controller;

import com.cliniradar.dto.AuthResponseDto;
import com.cliniradar.dto.LoginRequestDto;
import com.cliniradar.dto.RegisterRequestDto;
import com.cliniradar.entity.User;
import com.cliniradar.service.JwtService;
import com.cliniradar.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        User user = userService.register(requestDto);
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                requestDto.getEmail(),
                requestDto.getPassword()
        ));
        User user = userService.getByEmail(requestDto.getEmail());
        return ResponseEntity.ok(toResponse(user));
    }

    private AuthResponseDto toResponse(User user) {
        return new AuthResponseDto(jwtService.generateToken(user), user.getName(), user.getCrm());
    }
}
