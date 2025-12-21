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
    @Operation(
            summary = "List users (paginated)",
            description = "Returns a paginated list of users. Supports optional filtering by active status and free-text search via q.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    public ResponseEntity<Page<UserDto>> Users(
            @Parameter(description = "Pagination and sorting")
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @Operation(
            summary = "Create a user (admin)",
            description = "Creates a new user. Accepts multipart/form-data with a JSON part named 'user' and an optional 'avatar' file part.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., duplicate username/email/phone if enforced)")
            }
    )
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

    @Operation(
            summary = "Update a user (admin)",
            description = "Updates an existing user. Accepts multipart/form-data with a JSON part named 'user' and an optional 'avatar' file part.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., uniqueness constraint violation)")
            }
    )
    @PutMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    @Operation(
            summary = "Delete a user (admin)",
            description = "Deletes a user by id.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (user cannot be deleted due to references/constraints)")
            }
    )
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Activate a user account (admin)",
            description = "Sets the user's active flag to true.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User activated successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        adminService.setUserActive(id, true);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Deactivate a user account (admin)",
            description = "Sets the user's active flag to false.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminService.setUserActive(id, false);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "List translators (paginated)",
            description = "Returns a paginated list of translator profiles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/translators")
    public ResponseEntity<Page<TranslatorProfileDto>> translators(Pageable pageable) {
        return ResponseEntity.ok(adminService.listTranslators(pageable));
    }

    @GetMapping("/translators/{id}")
    @Operation(summary = "Get translator details")
    public ResponseEntity<TranslatorProfileDto> translator(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTranslator(id));
    }

    @Operation(
            summary = "Create a translator (admin)",
            description = "Creates a translator profile and related user data (if applicable). Accepts multipart/form-data with a JSON part named 'translator', and optional 'avatar' and 'docs' file parts. The operation is transactional.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Translator created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., duplicate identity/phone/email if enforced)")
            }
    )
    @PostMapping(value = "/translators", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranslatorProfileDto> createTranslator(
            @RequestPart("translator") @Valid AdminTranslatorDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "docs", required = false) MultipartFile[] docs
    ) {
        TranslatorProfileDto created = adminService.createTranslator(req, avatar, docs);
        return ResponseEntity.status(201).body(created);
    }

    @Operation(
            summary = "Update a translator (admin)",
            description = "Updates translator profile fields. Accepts multipart/form-data with a JSON part named 'translator', and optional 'avatar' and 'docs' file parts.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translator updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., invalid state transition / uniqueness constraint)")
            }
    )
    @PutMapping(value = "/translators/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranslatorProfileDto> updateTranslator(
            @PathVariable("id") Long translatorProfileId,
            @RequestPart("translator") @Valid AdminUpdateTranslatorDto req,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "docs", required = false) MultipartFile[] docs
    ) {
        TranslatorProfileDto updated = adminService.updateTranslator(translatorProfileId, req, avatar, docs);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete translator profile (admin)",
            description = "Deletes a translator profile by id.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Translator deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (translator cannot be deleted due to references/constraints)")
            }
    )
    @DeleteMapping("/translators/{id}")
    public ResponseEntity<Void> deleteTranslator(@PathVariable Long id) {
        adminService.deleteTranslator(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Approve a translator (admin)",
            description = "Approves a translator profile (moves status to APPROVED).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translator approved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (translator cannot be approved in current state)")
            }
    )
    @PutMapping("/translators/{id}/approve")
    public ResponseEntity<Void> approveTranslator(@PathVariable Long id) {
        adminService.approveTranslator(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Decline a translator (admin)",
            description = "Declines a translator profile (moves status to DECLINED).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translator declined successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (translator cannot be declined in current state)")
            }
    )
    @PutMapping("/translators/{id}/decline")
    public ResponseEntity<Void> declineTranslator(@PathVariable Long id) {
        adminService.declineTranslator(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "List calls (paginated)",
            description = "Returns a paginated list of calls.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Calls retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/calls")
    public ResponseEntity<Page<CallDto>> calls(Pageable pageable) {
        return ResponseEntity.ok(adminService.listCalls(pageable));
    }

    @Operation(
            summary = "Settle a call (charge client and pay interpreter)",
            description = "Settles a call by charging the client and paying the interpreter for the specified amount in KRW. Intended for admin/manual settlement flows.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Call settled successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters (e.g., amountWon <= 0)"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Call/user not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (duplicate settlement / invalid call state)")
            }
    )
    @PatchMapping("/calls/{callId}/settle")
    public ResponseEntity<Void> settleCall(@PathVariable String callId,
                                           @RequestParam Long clientId,
                                           @RequestParam Long interpreterId,
                                           @RequestParam BigDecimal amountWon) {
        depositService.chargeCallAndPay(clientId, interpreterId, callId, amountWon);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List withdrawals (paginated)",
            description = "Returns a paginated list of withdrawal requests. Optionally filter by status (e.g., PENDING/APPROVED/DECLINED).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawals retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/withdrawals")
    public ResponseEntity<Page<WithdrawalDto>> withdrawals(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listWithdrawals(pageable, status));
    }

    @Operation(
            summary = "Approve or decline a withdrawal (admin)",
            description = "Decides a withdrawal request. Use approve=true to approve, approve=false to decline. Optionally include an adminNote for audit/communication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawal decision saved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Withdrawal not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (withdrawal already decided / invalid state)")
            }
    )
    @PutMapping("/withdrawals/{id}/decide")
    public ResponseEntity<Void> decideWithdrawal(@PathVariable Long id,
                                                 @RequestParam boolean approve,
                                                 @RequestParam(required = false) String adminNote) {
        adminService.decideWithdrawal(id, approve, adminNote);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "List transactions for a user (admin)",
            description = "Returns a paginated list of transactions for the specified userId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionAdminDto>> transactions(
            @RequestParam Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listTransactions(userId, pageable));
    }

    @Operation(
            summary = "List themes (paginated)",
            description = "Returns a paginated list of themes. Supports optional free-text search via q.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Themes retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/themes")
    public ResponseEntity<Page<ThemeDto>> listThemes(@PageableDefault(size = 20) Pageable pageable,
                                                     @RequestParam(required = false) String q) {
        return ResponseEntity.ok(themeService.listThemes(pageable, q));
    }

    @Operation(
            summary = "Create a theme (admin)",
            description = "Creates a new theme. Accepts multipart/form-data with a JSON part named 'theme' and an optional 'icon' file part.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Theme created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., duplicate name if enforced)")
            }
    )
    @PostMapping(value = "/themes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDto> createTheme(
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(themeService.createTheme(dto, icon));
    }

    @Operation(
            summary = "Update a theme (admin)",
            description = "Updates an existing theme. Accepts multipart/form-data with a JSON part named 'theme' and an optional 'icon' file part.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Theme updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Theme not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., invalid state / uniqueness constraint)")
            }
    )
    @PutMapping(value = "/themes/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDto> updateTheme(
            @PathVariable Long id,
            @RequestPart("theme") @Valid ThemeDto dto,
            @RequestPart(value = "icon", required = false) MultipartFile icon
    ) {
        return ResponseEntity.ok(themeService.updateTheme(id, dto, icon));
    }

    @Operation(
            summary = "Delete a theme (admin)",
            description = "Deletes a theme by id.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Theme deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Theme not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (theme cannot be deleted due to references/constraints)")
            }
    )
    @DeleteMapping("/themes/{id}")
    public ResponseEntity<Void> deleteTheme(@PathVariable Long id) {
        themeService.deleteTheme(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List categories (paginated)",
            description = "Returns a paginated list of categories. Supports optional free-text search via q.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryDto>> listCategories(@PageableDefault(size = 20) Pageable pageable,
                                                            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(categoryService.listCategories(pageable, q));
    }

    @Operation(
            summary = "Create a category (admin)",
            description = "Creates a new category using the provided name as plain text in the request body.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Category created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid category name"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., duplicate category name if enforced)")
            }
    )
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody String categoryName) {
        return ResponseEntity.status(201).body(categoryService.createCategory(categoryName));
    }

    @Operation(
            summary = "Update a category (admin)",
            description = "Updates a category name using the provided name as plain text in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid category name"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., duplicate name if enforced)")
            }
    )
    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody String categoryName) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryName));
    }

    @Operation(
            summary = "Delete a category (admin)",
            description = "Deletes a category by id.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (category is in use by themes or other entities)")
            }
    )
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List calls by user (admin)",
            description = "Returns a paginated list of calls for the specified userId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Calls retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/users/{userId}/calls")
    public ResponseEntity<Page<CallDto>> callsByUser(@PathVariable Long userId,
                                                     @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCallsByUser(userId, pageable));
    }

    @Operation(
            summary = "List calls by translator (admin)",
            description = "Returns a paginated list of calls for the specified translator user id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Calls retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @GetMapping("/translators/{translatorUserId}/calls")
    public ResponseEntity<Page<CallDto>> callsByTranslator(@PathVariable Long translatorUserId,
                                                           @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCallsByTranslator(translatorUserId, pageable));
    }

    @Operation(
            summary = "List withdrawals by translator (admin)",
            description = "Returns a paginated list of withdrawals for the specified translator user id. Optionally filter by status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawals retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @GetMapping("/translators/{translatorUserId}/withdrawals")
    public ResponseEntity<Page<WithdrawalDto>> withdrawalsByTranslator(@PathVariable Long translatorUserId,
                                                                       @org.springframework.data.web.PageableDefault(size=20) Pageable pageable,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.listWithdrawalsByTranslator(translatorUserId, pageable, status));
    }

    @Operation(
            summary = "List deposits by user (admin)",
            description = "Returns a paginated list of deposit transactions for the specified userId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deposits retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/users/{userId}/deposits")
    public ResponseEntity<Page<TransactionAdminDto>> depositsByUser(@PathVariable Long userId,
                                                                    @org.springframework.data.web.PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listDepositsByUser(userId, pageable));
    }

    @Operation(
            summary = "List languages (paginated)",
            description = "Returns a paginated list of languages. Supports optional free-text filter by name via q.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Languages retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)")
            }
    )
    @GetMapping("/languages")
    public ResponseEntity<Page<LanguageDto>> listLanguages(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(languageService.listLanguages(pageable, q));
    }

    @Operation(
            summary = "Create a language (admin)",
            description = "Creates a new language using LanguageDto.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Language created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "409", description = "Conflict (duplicate language code/name if enforced)")
            }
    )
    @PostMapping("/languages")
    public ResponseEntity<LanguageDto> createLanguage(@RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(languageService.createLanguage(dto));
    }

    @Operation(
            summary = "Update a language (admin)",
            description = "Updates an existing language using LanguageDto.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Language updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Language not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (e.g., uniqueness violation / natural-id update not allowed)")
            }
    )
    @PutMapping("/languages/{id}")
    public ResponseEntity<LanguageDto> updateLanguage(@PathVariable Long id,
                                                      @RequestBody @jakarta.validation.Valid LanguageDto dto) {
        return ResponseEntity.ok(languageService.updateLanguage(id, dto));
    }

    @Operation(
            summary = "Delete a language (admin)",
            description = "Deletes a language by id. Returns 409 Conflict if the language is currently in use by other entities.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Language deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden (admin only)"),
                    @ApiResponse(responseCode = "404", description = "Language not found"),
                    @ApiResponse(responseCode = "409", description = "Conflict (language is in use)")
            }
    )
    @DeleteMapping("/languages/{id}")
    public ResponseEntity<Void> deleteLanguage(@PathVariable Long id) {
        languageService.deleteLanguage(id);
        return ResponseEntity.noContent().build();
    }

}
