package com.morago.backend.controller;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.dto.LanguageDto;
import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.admin.AdminTranslatorDto;
import com.morago.backend.dto.admin.AdminUpdateTranslatorDto;
import com.morago.backend.dto.admin.AdminUpdateUserDto;
import com.morago.backend.dto.admin.AdminUserDto;
import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import com.morago.backend.service.admin.AdminService;
import com.morago.backend.service.category.CategoryService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.language.LanguageService;
import com.morago.backend.service.theme.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only endpoints for management and moderation")
public class AdminController {

    private final AdminService adminService;
    private final FileService fileService;
    private final DepositService depositService;
    private final CategoryService categoryService;
    private final ThemeService themeService;
    private final LanguageService languageService;

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    @ApiResponse(responseCode = "200", description = "List of users retrieved successfully")
    public ResponseEntity<Page<UserDto>> Users(
            @Parameter(description = "Pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @PostMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> createUser(
            @RequestPart("user") @Valid AdminUserDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        UserDto created = adminService.createUser(req);

        if (avatar != null && !avatar.isEmpty()) {
            fileService.uploadAvatar(created.getId(), avatar);
            // created = adminService.getUser(created.getId());
        }
        return ResponseEntity.ok(created);
    }

    @PutMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a user (admin). Supports optional avatar upload.")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @RequestPart("user") @Valid AdminUpdateUserDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        UserDto updated = adminService.updateUser(id, req);

        if (avatar != null && !avatar.isEmpty()) {
            fileService.uploadAvatar(updated.getId(), avatar);
            // If your UserDto exposes avatarUrl, you can re-fetch to include the new URL:
            // updated = adminService.getUser(updated.getId());
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/activate")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        adminService.setUserActive(id, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminService.setUserActive(id, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/translators")
    @Operation(summary = "List all translators (paginated)")
    public ResponseEntity<Page<TranslatorProfileDto>> translators(Pageable pageable) {
        return ResponseEntity.ok(adminService.listTranslators(pageable));
    }

    @GetMapping("/translators/{id}")
    @Operation(summary = "Get translator details")
    public ResponseEntity<TranslatorProfileDto> translator(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTranslator(id));
    }

    @PostMapping(value = "/translators", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a translator (admin) with avatar and documents; fully transactional")
    public ResponseEntity<TranslatorProfileDto> createTranslator(
            @RequestPart("translator") @Valid AdminTranslatorDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "docs", required = false) MultipartFile[] docs
    ) {
        TranslatorProfileDto created = adminService.createTranslator(req, avatar, docs);
        return ResponseEntity.status(201).body(created);
    }


    @PutMapping(value = "/translators/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a translator (admin). Supports optional avatar and documents.")
    public ResponseEntity<TranslatorProfileDto> updateTranslator(
            @PathVariable("id") Long translatorProfileId,
            @RequestPart("translator") @Valid AdminUpdateTranslatorDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "docs", required = false) MultipartFile[] docs
    ) {
        TranslatorProfileDto updated = adminService.updateTranslator(translatorProfileId, req, avatar, docs);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/translators/{id}")
    @Operation(summary = "Delete translator profile")
    public ResponseEntity<Void> deleteTranslator(@PathVariable Long id) {
        adminService.deleteTranslator(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/translators/{id}/approve")
    @Operation(summary = "Approve a translator")
    public ResponseEntity<Void> approveTranslator(@PathVariable Long id) {
        adminService.approveTranslator(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/translators/{id}/decline")
    @Operation(summary = "Decline a translator")
    public ResponseEntity<Void> declineTranslator(@PathVariable Long id) {
        adminService.declineTranslator(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/calls")
    @Operation(summary = "List all calls (paginated)")
    public ResponseEntity<Page<CallDto>> calls(Pageable pageable) {
        return ResponseEntity.ok(adminService.listCalls(pageable));
    }

    @PatchMapping("/calls/{callId}/settle")
    public ResponseEntity<Void> settleCall(@PathVariable String callId,
                                           @RequestParam Long clientId,
                                           @RequestParam Long interpreterId,
                                           @RequestParam BigDecimal amountWon) {
        depositService.chargeCallAndPay(clientId, interpreterId, callId, amountWon);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/withdrawals")
    @Operation(summary = "List withdrawals (optionally filter by status)")
    public ResponseEntity<Page<WithdrawalDto>> withdrawals(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listWithdrawals(pageable, status));
    }

    @PutMapping("/withdrawals/{id}/decide")
    @Operation(summary = "Approve or decline a withdrawal")
    public ResponseEntity<Void> decideWithdrawal(@PathVariable Long id,
                                                 @RequestParam boolean approve,
                                                 @RequestParam(required = false) String adminNote) {
        adminService.decideWithdrawal(id, approve, adminNote);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/transactions")
    @Operation(summary = "List transactions for a user")
    public ResponseEntity<Page<TransactionAdminDto>> transactions(
            @RequestParam Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listTransactions(userId, pageable));
    }

    @GetMapping("/themes")
    public ResponseEntity<Page<ThemeDto>> listThemes(@PageableDefault(size = 20) Pageable pageable,
                                                     @RequestParam(required = false) String q) {
        return ResponseEntity.ok(themeService.listThemes(pageable, q));
    }

    @PostMapping(value = "/themes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new theme (with optional icon upload)")
    public ResponseEntity<ThemeDto> createTheme(
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(themeService.createTheme(dto, icon));
    }

    @PutMapping(value = "/themes/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a theme (with optional icon upload)")
    public ResponseEntity<ThemeDto> updateTheme(
            @PathVariable Long id,
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(themeService.updateTheme(id, dto, icon));
    }


    @DeleteMapping("/themes/{id}")
    @Operation(summary = "Delete a theme")
    public ResponseEntity<Void> deleteTheme(@PathVariable Long id) {
        themeService.deleteTheme(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryDto>> listCategories(@PageableDefault(size = 20) Pageable pageable,
                                                            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(categoryService.listCategories(pageable, q));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category")
    public ResponseEntity<?> createCategory(@RequestBody String categoryName) {
        return ResponseEntity.status(201).body(categoryService.createCategory(categoryName));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody String categoryName) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryName));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/calls")
    public ResponseEntity<Page<CallDto>> callsByUser(@PathVariable Long userId,
                                                     @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCallsByUser(userId, pageable));
    }

    @GetMapping("/translators/{translatorUserId}/calls")
    public ResponseEntity<Page<CallDto>> callsByTranslator(@PathVariable Long translatorUserId,
                                                           @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCallsByTranslator(translatorUserId, pageable));
    }

    @GetMapping("/translators/{translatorUserId}/withdrawals")
    public ResponseEntity<Page<WithdrawalDto>> withdrawalsByTranslator(@PathVariable Long translatorUserId,
                                                                       @org.springframework.data.web.PageableDefault(size=20) Pageable pageable,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.listWithdrawalsByTranslator(translatorUserId, pageable, status));
    }

    @GetMapping("/users/{userId}/deposits")
    public ResponseEntity<Page<TransactionAdminDto>> depositsByUser(@PathVariable Long userId,
                                                                    @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listDepositsByUser(userId, pageable));
    }


    @GetMapping("/languages")
    @io.swagger.v3.oas.annotations.Operation(summary = "List languages (paginated, optional name filter q)")
    public ResponseEntity<Page<LanguageDto>> listLanguages(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(languageService.listLanguages(pageable, q));
    }

    @PostMapping("/languages")
    @io.swagger.v3.oas.annotations.Operation(summary = "Create a language")
    public ResponseEntity<LanguageDto> createLanguage(@RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(languageService.createLanguage(dto));
    }

    @PutMapping("/languages/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Update a language")
    public ResponseEntity<LanguageDto> updateLanguage(@PathVariable Long id,
                                                      @RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(languageService.updateLanguage(id, dto));
    }

    @DeleteMapping("/languages/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Delete a language (409 if in use)")
    public ResponseEntity<Void> deleteLanguage(@PathVariable Long id) {
        languageService.deleteLanguage(id);
        return ResponseEntity.noContent().build();
    }

}
