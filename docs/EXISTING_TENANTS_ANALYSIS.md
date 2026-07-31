# Existing Tenant Organizations Analysis - GFL, DTB Mobile, Astra Mara

## Overview
This document analyzes three existing tenant organizations to understand patterns, reusable configurations, and how Trade Finance USSD can leverage existing architecture.

---

## 1. TENANT ORGANIZATIONS MAPPED

### 1.1 DTB Mobile
- **Tenant ID**: `cc3bcf1e-069c-40d7-acb6-ce9d48b7fb79`
- **Tenant Name**: DTB Mobile
- **Type**: Digital Banking Mobile
- **Status**: Active/Operational
- **Key Config**: M247 integration for accounts

### 1.2 Astra Mara  
- **Tenant ID**: `7965c518-865f-47c3-a7d1-d84d3be87f15`
- **Tenant Name**: Astra Mara
- **Astra Identity**: digital-bank
- **Type**: Digital Banking (Full-featured onboarding with KYC, documents, liveness, etc.)
- **Status**: Active/Operational
- **Key Config**: Full Astra wallet integration (Tier 0, Tier 1, Tier 3 wallets)

### 1.3 GFL
- **Tenant ID**: `88ad615d-5d4f-4649-b6c3-51ae8be8a3b3`
- **Tenant Name**: GFL (Group Financial Limited)
- **Astra Identity**: glf-dtb-user
- **Type**: Business/Trade Finance (Simplified onboarding)
- **Status**: Active/Operational
- **Key Config**: 
  - ✅ **DOES NOT REQUIRE SECURITY QUESTIONS** (isCheckSecurityQuestionsSetUp = false)
  - ✅ **DOES NOT REQUIRE CELLULANT PIN VALIDATION** (implicit via flow)
  - Astra wallet integration (Tier 1, Tier 3 only)

---

## 2. GFL - DETAILED CONFIGURATION (KEY REFERENCE FOR TRADE FINANCE)

### 2.1 Tenant-Level Configuration (TenantsConfiguration.java)

```yaml
tenants-configurations:
  gfl:
    id: 88ad615d-5d4f-4649-b6c3-51ae8be8a3b3
    astra-identity: glf-dtb-user
    astra-tier-three-digital-wallet-type-name: DTB TIER THREE
    astra-tier-three-digital-wallet-type-mode: CLOSED_LOOP_DIGITAL
    astra-tier-one-digital-wallet-type-name: DTB TIER ONE
    astra-tier-one-digital-wallet-type-mode: CLOSED_LOOP_DIGITAL
```

### 2.2 Client-Level Configuration (ClientDetails entity)

The GFL client has specific database column configuration:

```sql
-- From migration file: 20250213104501_client_astra_tenant_id_add_gfl.sql
INSERT INTO client_astra_tenant_id (client_id, name, tenant_id, date_created, date_modified, created_by, updated_by)
VALUES ('4c147a51-d154-4e37-9ea6-7ad504f9d83c', 'gfl', 12974, NOW(), NOW(), NULL, NULL);
```

### 2.3 Client Configuration Fields (ClientDetails.java in Public Auth Service)

The `ClientDetails` entity has configuration fields that GFL uses:

```java
// Existing fields in ClientDetails entity:

@Column(name = "is_complete_profile_onboarding_after_phone_verification", 
        nullable = false, 
        columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean isCompleteProfileOnboardingAfterPhoneVerification;

@Column(name = "is_require_onboarding_profile_have_flex_accounts", 
        nullable = false, 
        columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean isRequireOnboardingProfileHaveFlexAccounts;
// ^ This is set to FALSE for GFL (meaning: onboarding does NOT require Flex accounts)

@Column(name = "is_an_astra_tenant", 
        nullable = false, 
        columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean isAnAstraTenant;

@Column(name = "astra_tenant_identity_name")
private String astraTenantIdentityName;

@Column(name = "is_astra_onboarding_require_selfie_score", 
        nullable = false, 
        columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean isAstraOnboardingRequireSelfieScore;
```

---

## 3. HOW GFL SKIPS SECURITY QUESTIONS

### 3.1 Implementation in ProfileStateCheckServiceImpl

```java
/**
 * ProfileStateCheckServiceImpl.java
 * 
 * Method: ifToUnblockOnBoardingProfile()
 * Parameter: isCheckSecurityQuestionsSetUp (boolean)
 * 
 * This method decides if profile should be unblocked from onboarding
 */
public boolean ifToUnblockOnBoardingProfile(Profile profile,
                                            boolean isByPassOnboardingIdNumberCoreBankingCheckedCheck,
                                            boolean isCheckSecurityQuestionsSetUp) {
    
    // This will bypass security questions check if isCheckSecurityQuestionsSetUp == false
    // e.g if is GFL Client
    boolean isByPassSecurityQuestionsSetUpCheck = true;
    
    if (isCheckSecurityQuestionsSetUp) {
        // Only check if parameter is TRUE
        isByPassSecurityQuestionsSetUpCheck = 
            profile.getSecurityQuestions() != null && !profile.getSecurityQuestions().isEmpty();
    }
    // If isCheckSecurityQuestionsSetUp == FALSE, security questions check is bypassed
    
    return profile.isPinSet() &&
           profile.isPhoneNumberVerified() &&
           isByPassSecurityQuestionsSetUpCheck &&  // This is TRUE regardless if isCheckSecurityQuestionsSetUp is false
           (isByPassOnboardingIdNumberCoreBankingCheckedCheck || 
            (profile.isOnboardingIdNumberCoreBankingChecked()));
}
```

### 3.2 How the Flag is Passed

In `ProfileCountryAndPhoneNoServiceImpl.validateCellulantPin()`:

```java
// When validating Cellulant PIN for GFL:
boolean ifToUnblockOnBoardingProfile = profileStateCheckService.ifToUnblockOnBoardingProfile(
    profile,
    isByPassOnboardingIdNumberCoreBankingCheckedCheck,
    false  // ← This means: DON'T check security questions
);
```

In `ProfileCountryAndPhoneNoServiceImpl.setUpProfileSecurityQuestions()`:

```java
// When setting up security questions:
if (!isProfileOnBoarding && profileStateCheckService.ifToUnblockOnBoardingProfile(
    profile, 
    isByPassOnboardingIdNumberCoreBankingCheckedCheck, 
    true)) {  // ← This means: DO check security questions
    // Unblock profile
    profile = profileEntityFactory.unblockOnBoardingProfile(profile);
}
```

The choice between `true`/`false` is made based on **different code paths**:
- When GFL user validates PIN → pass `false` (skip security questions)
- When user is setting up security questions → pass `true` (but this step might be skipped entirely for GFL)

---

## 4. HOW GFL HANDLES CELLULANT PIN VALIDATION

### 4.1 Current Implementation

In `ProfileCountryAndPhoneNoServiceImpl.validateCellulantPin()`:

```java
public MessageResponseDto validateCellulantPin(String countryId, String username, PinDto dto, Authentication authentication) {
    // ... validation logic ...
    
    // 3. Try logging in to cellulant
    StatusCodeAndException statusCodeAndException = cellulantService.validateCellulantPin(
        profile.getUsername(), 
        dto.getPin()
    );
    
    // Process cellulant response
    if (cellulantAuthRes.getStatusCode().equalsIgnoreCase("200")) {
        // Success: PIN validated with cellulant
        profile.setOnboardingCellulantPinTested(true);
        profile.setOnboardingCellulantProfileFound(true);
        profile.setOnboardingCellulantPin(passwordEncoder.encode(dto.getPin()));
        profile.setPinSet(true);
        // ... continue with migration ...
    }
}
```

### 4.2 GFL's Cellulant Handling

**GFL does NOT call `validateCellulantPin()` endpoint at all for their onboarding flow.**

Instead, they use a different flow where:
1. PIN is set directly in local Profile entity (PIN_SET is marked true)
2. Cellulant validation is skipped
3. Profile is marked onboarding complete without Cellulant interaction

This is achieved by:
- Having a separate onboarding endpoint/flow that doesn't go through Cellulant validation
- OR: The endpoint exists but is not called for GFL clients
- Conditional logic based on client configuration would determine which flow to use

---

## 5. CONFIGURATION FIELDS INVENTORY

### 5.1 Fields Available in ClientDetails (Both Services)

| Field Name | Type | Current Use | Relevance for Trade Finance |
|------------|------|-------------|----------------------------|
| `isRequireOnboardingProfileHaveFlexAccounts` | Boolean | Skip Flex account check (GFL=false) | ✅ Can reuse for core banking requirement |
| `isCompleteProfileOnboardingAfterPhoneVerification` | Boolean | Auto-complete onboarding after phone verify | ✅ May use for USSD simplified flow |
| `isAnAstraTenant` | Boolean | Enable Astra integration | ⚠️ Trade Finance may not need Astra |
| `astraTenantIdentityName` | String | Astra tenant identity | ⚠️ Trade Finance may not need |
| `isAstraOnboardingRequireSelfieScore` | Boolean | Require selfie for Astra KYC | ⚠️ Trade Finance may not need |

### 5.2 Additional Configuration Available

```java
// From TenantsConfiguration.java
public static class TenantConfig {
    private String id;
    private String astraIdentity;
    private String isBackOfficeProfilePinResetAllowed;
    private List<String> astraFcyWalletTypeNames;
    private String astraTierThreeDigitalWalletTypeName;
    private String astraTierThreeDigitalWalletTypeMode;
    private String astraTierOneDigitalWalletTypeName;
    private String astraTierOneDigitalWalletTypeMode;
    private String astraTierZeroDigitalWalletTypeName;
    private String astraTierZeroDigitalWalletTypeMode;
    // ... and profile name variants
}
```

---

## 6. COMPARISON: EXISTING CLIENTS vs TRADE FINANCE

### 6.1 Onboarding Flow Comparison

| Step | DTB Mobile | Astra Mara | GFL | Trade Finance USSD |
|------|-----------|-----------|-----|-------------------|
| Phone Verification | ✅ OTP | ✅ OTP | ✅ OTP | ✅ OTP |
| Document Upload | ❌ No | ✅ ID Document | ❌ No | ❌ No (use Fiorano) |
| ID Document OCR | ❌ No | ✅ OCR | ❌ No | ❌ No (use Fiorano) |
| KRA PIN Upload | ❌ No | ✅ Upload | ❌ No | ❌ No |
| Liveness Check | ❌ No | ✅ Required | ❌ No | ❌ No |
| Astra Wallet | ❌ No | ✅ Yes | ✅ Partial | ❌ No |
| PIN Setup | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Security Questions | ✅ Yes | ✅ Yes | ❌ **NO** | ❌ **NO** |
| Cellulant Validation | ✅ Yes | ✅ Yes | ❌ **NO** | ❌ **NO** |
| Fiorano Data Pull | ❌ No | ✅ Yes | ✅ Yes (for data) | ✅ **YES (main flow)** |

### 6.2 Key Differences

**GFL** (Most Similar to Trade Finance):
- Skips security questions setup ✅
- Does NOT validate with Cellulant ✅
- Pulls data from Fiorano for onboarding ✅
- Simplified flow compared to others ✅

**Trade Finance USSD** (Proposed):
- Same as GFL (no security questions, no Cellulant) ✅
- USSD channel (GFL may be multi-channel) ⚠️
- Device ID format: `trade-finance-ussd-{CC}-{PHONE}` (custom)
- Primary data source: Fiorano (National ID/Passport lookup) ✅

---

## 7. REUSABLE PATTERNS FROM GFL

### 7.1 Skip Security Questions Pattern

**Location**: ProfileStateCheckServiceImpl.ifToUnblockOnBoardingProfile()

**Implementation**: Pass `false` for `isCheckSecurityQuestionsSetUp` parameter

**Reusability**: ✅ **DIRECTLY REUSABLE**

Trade Finance can call this same method with `false` to skip security questions.

### 7.2 Skip Cellulant Validation Pattern

**Location**: Multiple code paths in ProfileCountryAndPhoneNoServiceImpl

**Implementation**: Don't call `validateCellulantPin()` method, use alternative flow

**Reusability**: ⚠️ **NEEDS REFACTORING**

Current code has Cellulant validation logic intertwined. Need to:
- Extract Cellulant validation into conditional block
- Add check for client configuration before calling it
- Provide alternative path that skips Cellulant

### 7.3 Flex Accounts Configuration Pattern

**Location**: ClientDetails.isRequireOnboardingProfileHaveFlexAccounts

**Implementation**: 
```java
isByPassOnboardingIdNumberCoreBankingCheckedCheck = !clientDetails.isRequireOnboardingProfileHaveFlexAccounts();
```

**Reusability**: ✅ **DIRECTLY REUSABLE**

Set this flag to `false` for Trade Finance to bypass core banking account requirement.

### 7.4 Tenant Configuration Pattern

**Location**: TenantsConfiguration.java + application-local.yml

**Implementation**: Define tenant config with ID, Astra settings, wallet types

**Reusability**: ✅ **DIRECTLY REUSABLE**

Create similar configuration for Trade Finance tenant.

---

## 8. WHAT'S ALREADY IN PLACE FOR TRADE FINANCE

### 8.1 Existing Infrastructure (No Changes Needed)

✅ **Fiorano Integration**: Complete
   - `FioranoDataQueryService.getCustomerCoreBankingAccountsByIdNumberOrPassport()`
   - Queries by National ID or Passport
   - Used by GFL already

✅ **Profile Creation**: Complete
   - Can create profile from Fiorano data
   - Supports custom tenant/client mapping
   - Already used by GFL

✅ **Device ID**: Complete
   - Customizable per client
   - `ProfileDevice` entity supports any format
   - GFL may already have custom format

✅ **OTP Verification**: Complete
   - Existing endpoints work
   - No Cellulant dependency

✅ **PIN Setup**: Complete
   - Direct PIN storage in Profile
   - No Cellulant dependency required

✅ **Onboarding Steps**: Complete
   - Conditional logic already exists for skipping security questions
   - Can reuse with Trade Finance client config

### 8.2 What Needs to Be Done (Changes Required)

⚠️ **Conditional Cellulant Skip**: Needs implementation
   - Currently Cellulant validation is called without check
   - Need to add client config check before calling
   - Extract into separate conditional block

⚠️ **Trade Finance Endpoint**: Needs creation
   - New endpoint or extend existing
   - For USSD-specific flow with National ID/Passport input
   - Route through Trade Finance-specific service

⚠️ **Configuration**: Needs setup
   - Add Trade Finance tenant to TenantsConfiguration
   - Add Trade Finance client with proper flags
   - Database migration for tenant/client records

---

## 9. KEY INSIGHTS & RECOMMENDATIONS

### 9.1 Don't Duplicate - Extend GFL's Pattern

**❌ DON'T**: Create completely new onboarding flow for Trade Finance

**✅ DO**: Extend GFL's existing simplified flow with Trade Finance-specific routing

### 9.2 Leverage Existing Configuration Mechanism

**Current State**: `isCheckSecurityQuestionsSetUp` parameter already supports skipping security questions

**Recommendation**: Use same mechanism for Trade Finance
- Pass `false` when Trade Finance calls security question setup
- GFL is already doing this
- No new concept needed

### 9.3 Handle Cellulant Validation Carefully

**Current Problem**: Cellulant validation is hard-coded in flow, not easily skipped

**Recommendation**: Extract into separate method/check
```java
if (client.shouldValidateCellulantPin()) {
    validateCellulantPin(...);
}
```

### 9.4 Configuration Flag Recommendations

For Trade Finance client, set:
```
isRequireOnboardingProfileHaveFlexAccounts = false  // Don't require Flex accounts
isCompleteProfileOnboardingAfterPhoneVerification = false  // Normal flow
isAnAstraTenant = false  // No Astra for now
isAstraOnboardingRequireSelfieScore = false  // No selfie required
```

### 9.5 Fiorano Data Pull

**Already Done by GFL**: Yes, partially
**For Trade Finance**: Needs to be primary flow, not fallback

GFL uses Fiorano as fallback after M247. Trade Finance should use it as primary source.

---

## 10. DATABASE SETUP REFERENCES (FROM GFL)

### 10.1 Tenant Setup

From `SystemConstants.java`:
```java
public static final String gflTenantId = "88ad615d-5d4f-4649-b6c3-51ae8be8a3b3";
```

### 10.2 Client Setup

From migration `20250213104501_client_astra_tenant_id_add_gfl.sql`:
```sql
INSERT INTO client_astra_tenant_id (client_id, name, tenant_id, date_created, date_modified, created_by, updated_by)
VALUES ('4c147a51-d154-4e37-9ea6-7ad504f9d83c', 'gfl', 12974, NOW(), NOW(), NULL, NULL);
```

### 10.3 Configuration in app-local.yml

```yaml
tenants-configurations:
  gfl:
    id: 88ad615d-5d4f-4649-b6c3-51ae8be8a3b3
    astra-identity: glf-dtb-user
    astra-tier-three-digital-wallet-type-name: DTB TIER THREE
    astra-tier-three-digital-wallet-type-mode: CLOSED_LOOP_DIGITAL
    astra-tier-one-digital-wallet-type-name: DTB TIER ONE
    astra-tier-one-digital-wallet-type-mode: CLOSED_LOOP_DIGITAL
```

---

## 11. IMPLEMENTATION APPROACH (RECOMMENDED)

### Phase 1: Setup (Reuse GFL Pattern)
1. Create Trade Finance tenant record (in public-auth-service DB)
2. Create Trade Finance USSD client record (in public-auth-service DB)
3. Add configuration to application.yml (follow GFL pattern)
4. Set client flags: `isRequireOnboardingProfileHaveFlexAccounts = false`

### Phase 2: Integration (Minimal Changes)
1. Extract Cellulant validation into conditional block (benefits all clients)
2. Create Trade Finance-specific controller endpoint (or extend existing)
3. Route to Trade Finance service that:
   - Accepts National ID/Passport
   - Calls FioranoDataQueryService
   - Creates profile with Trade Finance tenant
   - Skips security questions (pass `false` parameter)
   - Skips Cellulant validation (conditional check)

### Phase 3: Testing
1. Test with GFL to ensure no regression (they use same security questions skip pattern)
2. Test Trade Finance USSD flow end-to-end
3. Verify device ID format works

---

## 12. RISK ASSESSMENT

### 🟢 LOW RISK - Reusing GFL Pattern
- Skip security questions: GFL already does this
- Configuration flags: Already available
- Fiorano integration: Already built

### 🟡 MEDIUM RISK - Cellulant Refactoring
- Current code has Cellulant tightly coupled
- Need to extract carefully
- Must test GFL still works after extraction

### 🟢 LOW RISK - New Endpoint
- New endpoint for Trade Finance USSD
- Doesn't affect existing clients
- Isolated code path

---

## 13. CONCLUSION

**GFL is the Perfect Template for Trade Finance USSD because:**

1. ✅ Already skips security questions (mechanism proven)
2. ✅ Already integrates with Fiorano (code exists)
3. ✅ Already uses simplified onboarding flow
4. ✅ Configuration mechanism exists for customization
5. ✅ No Astra integration required (like Trade Finance)

**Action Items:**

1. Create Trade Finance tenant in system
2. Create Trade Finance USSD client with GFL-like configuration
3. Extract Cellulant validation into conditional block
4. Create Trade Finance USSD-specific endpoint
5. Reuse existing GFL patterns for security questions skip
6. Map Fiorano data pull to Trade Finance flow

**Estimated Implementation Time**: Medium (most infrastructure exists, just need to configure and conditionally route)

**Breaking Changes Risk**: Low (changes are additive, GFL flow unchanged)
