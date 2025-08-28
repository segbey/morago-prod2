package com.morago.backend.service.user;

import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserRegistrationResponseDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import com.morago.backend.entity.UserProfile;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.exception.password.PasswordMismatchException;
import com.morago.backend.exception.password.PasswordRequiredException;
import com.morago.backend.exception.phonenumber.PhoneAlreadyExistsException;
import com.morago.backend.exception.phonenumber.PhoneInvalidException;
import com.morago.backend.exception.UserNotFoundException;
import com.morago.backend.exception.password.WeakPasswordException;
import com.morago.backend.exception.password.WrongOldPasswordException;
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
    public UserRegistrationResponseDto registerUser(UserRegistrationRequestDto dto) {
        return register(dto, Roles.ROLE_USER);
    }

    @Override
    @Transactional
    public UserRegistrationResponseDto registerTranslator(UserRegistrationRequestDto dto) {
        return register(dto, Roles.ROLE_TRANSLATOR);
    }

    @Transactional
    private UserRegistrationResponseDto register(UserRegistrationRequestDto dto, Roles fixedRole) {
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
    public UserUpdateProfileResponseDto updateProfile(Long userId, UserUpdateProfileRequestDto dto) {
        User user = findByIdOrThrow(userId);

        boolean changed = false;

        if (dto.getFirstName() != null) {
            String v = dto.getFirstName().trim();
            String newVal = v.isEmpty() ? null : v;
            if (!java.util.Objects.equals(user.getFirstName(), newVal)) {
                user.setFirstName(newVal);
                changed = true;
            }
        }
        if (dto.getLastName() != null) {
            String v = dto.getLastName().trim();
            String newVal = v.isEmpty() ? null : v;
            if (!java.util.Objects.equals(user.getLastName(), newVal)) {
                user.setLastName(newVal);
                changed = true;
            }
        }

        if (changed) {
            user = userRepository.save(user);
        }
        return userMapper.toUpdateProfileResponseDto(user);
    }


    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto dto) {
        User user = findByIdOrThrow(userId);

        boolean isAdmin = currentUserIsAdmin();

        if (!isAdmin) {
            if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
                throw new WrongOldPasswordException();
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new WrongOldPasswordException();
            }
        }

        if (!java.util.Objects.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        validatePasswordStrength(dto.getNewPassword());

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
    }

    private boolean currentUserIsAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        return a.getAuthorities().stream().anyMatch(ga -> "ROLE_ADMIN".equals(ga.getAuthority()));
    }

    private void validatePasswordStrength(String pwd) {
        if (pwd == null || pwd.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters");
        }
        // Example: min 1 letter и 1 digit
         if (!pwd.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
             throw new WeakPasswordException("Password must contain letters and digits");
         }
    }

    //CHANGE IT AS ADMIN'S PART
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserRegistrationResponseDto createUser(UserRegistrationRequestDto dto) {
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

