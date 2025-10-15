package com.morago.backend.controller;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.dto.LanguageDto;
import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.admin.AdminTranslatorDto;
import com.morago.backend.dto.admin.AdminUserDto;
import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import com.morago.backend.service.admin.AdminService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.file.FileService;
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
import org.springframework.web.bind.annotation.*;
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
    @Operation(summary = "Create a user (admin) with optional avatar upload")
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

//    @PutMapping("/users/{id}")
//    @Operation(summary = "Update an existing user")
//    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
//        return ResponseEntity.ok(adminService.updateUser(id, userDto));
//    }

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

//    @PostMapping("/translators")
//    @Operation(summary = "Create a new translator profile")
//    public ResponseEntity<TranslatorProfileDto> createTranslator(@RequestBody TranslatorProfileDto dto) {
//        return ResponseEntity.status(201).body(adminService.createTranslator(dto));
//    }

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

//    @PutMapping("/translators/{id}")
//    @Operation(summary = "Update translator profile")
//    public ResponseEntity<TranslatorProfileDto> updateTranslator(@PathVariable Long id, @RequestBody TranslatorProfileDto dto) {
//        return ResponseEntity.ok(adminService.updateTranslator(id, dto));
//    }

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
        return ResponseEntity.ok(adminService.listThemes(pageable, q));
    }

    @PostMapping(value = "/themes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new theme (with optional icon upload)")
    public ResponseEntity<ThemeDto> createTheme(
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(adminService.createTheme(dto, icon));
    }

    @PutMapping(value = "/themes/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a theme (with optional icon upload)")
    public ResponseEntity<ThemeDto> updateTheme(
            @PathVariable Long id,
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(adminService.updateTheme(id, dto, icon));
    }

//    @PostMapping("/themes")
//    @Operation(summary = "Create a new theme")
//    public ResponseEntity<ThemeDto> createTheme(@Valid @RequestBody ThemeDto dto) {
//        return ResponseEntity.ok(adminService.createTheme(dto));
//    }
//
//    @PutMapping("/themes/{id}")
//    @Operation(summary = "Update a theme")
//    public ResponseEntity<ThemeDto> updateTheme(@PathVariable Long id, @Valid @RequestBody ThemeDto dto) {
//        return ResponseEntity.ok(adminService.updateTheme(id, dto));
//    }

    @DeleteMapping("/themes/{id}")
    @Operation(summary = "Delete a theme")
    public ResponseEntity<Void> deleteTheme(@PathVariable Long id) {
        adminService.deleteTheme(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryDto>> listCategories(@PageableDefault(size = 20) Pageable pageable,
                                                            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(adminService.listCategories(pageable, q));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category")
    public ResponseEntity<?> createCategory(@RequestBody String categoryName) {
        return ResponseEntity.status(201).body(adminService.createCategory(categoryName));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody String categoryName) {
        return ResponseEntity.ok(adminService.updateCategory(id, categoryName));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
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
        return ResponseEntity.ok(adminService.listLanguages(pageable, q));
    }

    @PostMapping("/languages")
    @io.swagger.v3.oas.annotations.Operation(summary = "Create a language")
    public ResponseEntity<LanguageDto> createLanguage(@RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(adminService.createLanguage(dto));
    }

    @PutMapping("/languages/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Update a language")
    public ResponseEntity<LanguageDto> updateLanguage(@PathVariable Long id,
                                                      @RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(adminService.updateLanguage(id, dto));
    }

    @DeleteMapping("/languages/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Delete a language (409 if in use)")
    public ResponseEntity<Void> deleteLanguage(@PathVariable Long id) {
        adminService.deleteLanguage(id);
        return ResponseEntity.noContent().build();
    }

}
