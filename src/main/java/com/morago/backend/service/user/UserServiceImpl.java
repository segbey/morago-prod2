package com.morago.backend.service.user;

import com.morago.backend.dto.UserProfileDto;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import com.morago.backend.entity.UserProfile;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.exception.PasswordMismatchException;
import com.morago.backend.exception.PasswordRequiredException;
import com.morago.backend.exception.PhoneAlreadyExistsException;
import com.morago.backend.exception.PhoneInvalidException;
import com.morago.backend.exception.UserNotFoundException;
import com.morago.backend.mapper.UserMapper;
import com.morago.backend.repository.RefreshTokenRepository;
import com.morago.backend.repository.UserRepository;
import com.morago.backend.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(id)));
    }

    @Override
    public User getCurrentUser() {
        String username = getAuthUsername();
        return findByUsernameOrThrow(username);
    }

    private String getAuthUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException("Unauthenticated");
        }
        return auth.getName();
    }

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRegistrationRequestDto dto) {
        return register(dto, Roles.ROLE_USER);
    }

    @Override
    @Transactional
    public UserResponseDto registerTranslator(UserRegistrationRequestDto dto) {
        return register(dto, Roles.ROLE_TRANSLATOR);
    }

    @Transactional
    private UserResponseDto register(UserRegistrationRequestDto dto, Roles fixedRole) {
        String phone = validateRegistration(dto);

        User user = userMapper.toEntity(dto);
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActive(true);

        user.setRoles(new java.util.HashSet<>(
                java.util.List.of(roleService.getRoleOrThrow(fixedRole))
        ));

        switch (fixedRole) {
            case ROLE_USER -> user.setUserProfile(UserProfile.builder().build());
            case ROLE_TRANSLATOR -> user.setTranslatorProfile(TranslatorProfile.builder().build());
        }

        return userMapper.toResponseDto(userRepository.save(user));
    }

    private String toKoreanMobile010Strict(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (!s.matches("^\\d{11}$")) return null;
        if (!s.startsWith("010")) return null;
        return s;
    }

    private String validateRegistration(UserRegistrationRequestDto dto) {
        String phone = toKoreanMobile010Strict(dto.getPhoneNumber());
        if (phone == null) throw new PhoneInvalidException(dto.getPhoneNumber());

        String pwd = dto.getPassword();
        if (pwd == null || pwd.isBlank()) throw new PasswordRequiredException();

        if (!pwd.equals(dto.getConfirmPassword())) throw new PasswordMismatchException();

        if (userRepository.existsByUsername(phone)) {
            throw new PhoneAlreadyExistsException(phone);
        }

        dto.setPhoneNumber(phone);
        return phone;
    }


    //for users
    @Override
    @Transactional
    public UserResponseDto updateProfile(Long userId, UserProfileDto dto) {
        User user = findByIdOrThrow(userId);

        if (dto.getFirstName() != null) {
            String v = dto.getFirstName().trim();
            user.setFirstName(v.isEmpty() ? null : v);
        }
        if (dto.getLastName() != null) {
            String v = dto.getLastName().trim();
            user.setLastName(v.isEmpty() ? null : v);
        }

        return userMapper.toResponseDto(userRepository.save(user));
    }


    //CHANGE IT AS ADMIN'S PART
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponseDto createUser(UserRegistrationRequestDto dto) {
//        if (userRepository.existsByUsername(dto.getUsername())) {
//            throw new IllegalArgumentException("Username already exists");
//        }
//        User user = userMapper.toEntity(dto);
//
//        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
//            throw new IllegalArgumentException("Password is required for new user");
//        }
//        user.setPassword(passwordEncoder.encode(dto.getPassword()));

//        user.setRoles(resolveRoles(dto.getRoles()));

//        return userMapper.toResponseDto(userRepository.save(user));
        return null;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("ID " + id));
     //   userRepository.deleteById(id);
        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }
}

