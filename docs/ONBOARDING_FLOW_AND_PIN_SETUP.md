# Trade Finance USSD: Onboarding Flow & PIN Setup Journey

## Overview
This document maps the complete onboarding flow for Trade Finance USSD, specifically showing:
1. How Fiorano core banking data is pulled
2. How the linking of customer accounts works
3. How PIN setup flows during onboarding
4. Which endpoints are involved and in what sequence

---

## ⚠️ CRITICAL: Always Start with GET Request

**This is the most important step - DO NOT skip this.**

### The GET Method (ALWAYS First)

```
GET /v1/profiles/country/{countryId}/phone-no/{phoneNo}

No Body Required. No Authentication Required.

Possible Responses:

1️⃣  Response 200 OK (Profile exists):
{
  "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
  "username": "254711111111",
  "isBlocked": true,
  "blockReason": "Onboarding",
  "blockReasonDescription": "Customer is currently onboarding",
  "pinSet": false,
  "onboardingStepProgress": "PinSetUp"
}

2️⃣  Response 404 NOT FOUND (Profile does not exist):
{
  "responseCode": "404",
  "responseDescription": "Profile not found"
}
```

### Decision Tree (Must Follow This)

```
┌─────────────────────────────────────────────┐
│  Call GET /v1/profiles/.../phone-no/{phone} │
└────────────────┬────────────────────────────┘
                 │
         ┌───────┴───────┐
         │               │
    ┌────▼────┐     ┌────▼────┐
    │ 200 OK  │     │ 404 404  │
    └────┬────┘     └────┬────┘
         │               │
         ├─ isBlocked?   └──→ Profile doesn't exist
         │                   └──→ CREATE NEW (Phase 1: POST)
         │
         ├─ false ────────→ ✅ Already onboarded
         │                 └──→ Skip onboarding
         │                 └──→ Show main menu
         │
         └─ true ────────→ ⏸️ Onboarding interrupted
                          └──→ Resume from: onboardingStepProgress
                          └──→ Most likely: PinSetUp (Phase 4: PATCH)
```

### Why GET First (Read This!)

| Issue | GET First | Without GET |
|-------|-----------|------------|
| **Duplicate Profiles** | ✅ Prevented (check first) | ❌ Risk of creating duplicates |
| **Resume Interrupted** | ✅ Knows exact step (onboardingStepProgress) | ❌ Loses progress, starts over |
| **Side Effects** | ✅ None (read-only) | ❌ Creates data you don't need |
| **User Experience** | ✅ Skip onboarding if done | ❌ Force re-onboarding already done users |
| **Fiorano Calls** | ✅ Efficient (only when needed) | ❌ Wasteful (might pull twice) |

### Code Reference

| Property | Value |
|----------|-------|
| **Controller** | ProfileCountryAndPhoneNoV1Controller.java |
| **Line Number** | 155 |
| **Method Name** | `getProfileByUsername(countryId, phoneNo, authentication)` |
| **Service** | ProfileCountryAndPhoneNoServiceImpl.java |
| **Service Lines** | 686-716 |
| **Return Type** | ProfileBlockInfoDTO |
| **HTTP Status** | 200 (found) or 404 (not found) |
| **Request Body** | None |
| **Authentication** | Required (@PreAuthorize) |

---

## Onboarding Steps (Enum: `OnboardingStepName`)

The system has defined onboarding steps that are tracked during the journey:

```
PhoneVerification          ← First step
StaticDataEntry            
IprsCheck                  
IdDocumentUpload           
IdDocumentOcrFront         
IdDocumentOcrBack          
KraPinUpload               
KraPinOcr                  
LivenessCheck              
AstraWalletCreation        
AccountOpening             
PinSetUp                   ← PIN Setup happens here
SecurityQuestionsSetUp     ← SKIPPED for Trade Finance
```

---

## Complete Onboarding Journey for Trade Finance USSD

### **PRE-PHASE: Profile Existence Check** ⭐ CRITICAL FIRST STEP

**Before attempting onboarding, ALWAYS check if the profile already exists.**

#### Endpoint 0: Check Profile Existence
```
GET /v1/profiles/country/{countryId}/phone-no/{phoneNo}

No Body Required

Response 200 OK (Profile Exists):
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "username": "254711111111",
    "isBlocked": true,
    "blockReason": "Onboarding",
    "blockReasonDescription": "Customer is currently onboarding",
    "pinSet": false,
    "onboardingStepProgress": "PinSetUp"  // Resume from this step
  },
  "responseCode": "200",
  "responseDescription": "Profile retrieved successfully"
}

Response 404 NOT FOUND (Profile Does Not Exist):
{
  "responseCode": "404",
  "responseDescription": "Profile not found"
}
```

**Decision Logic:**
```
IF GET returns 200 OK:
  ├─ IF isBlocked == false
  │  └─ User is already onboarded → Skip onboarding, show main menu
  │
  └─ IF isBlocked == true AND blockReason == "Onboarding"
     └─ User started onboarding but didn't complete
        └─ Resume from onboardingStepProgress step (e.g., "PinSetUp")

IF GET returns 404:
  └─ Profile doesn't exist
     └─ Proceed to PHASE 1: POST to create new profile
```

**Code Reference:**
- **Controller:** ProfileCountryAndPhoneNoV1Controller.java, Line 155
- **Method:** `getProfileByUsername(countryId, phoneNo, authentication)`
- **Service:** ProfileCountryAndPhoneNoServiceImpl.java, Line 686-716
- **Returns:** ProfileBlockInfoDTO with onboarding state

**Why GET First:**
1. ✅ Read-only operation (no side effects)
2. ✅ Checks current state before making changes
3. ✅ Allows resuming interrupted onboarding
4. ✅ Prevents duplicate profile creation
5. ✅ No risk of data corruption

---

### **PHASE 1: Profile Creation & Phone Verification**

#### Endpoint 1: Create Profile & Verify Phone
```
POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}

Body:
{
  "isExistingCustomer": false,
  "isAcceptTermsAndConditions": true,
  "isAcceptMarketingConsent": true,
  "deviceInfo": {
    "id": "trade-finance-ussd-KE-254711111111",  // Device ID format
    "deviceName": "USSD Device",
    "deviceType": "USSD",
    "deviceOs": "USSD",
    "deviceOsVersion": "1.0",
    "deviceModel": "USSD",
    "buildVersion": "1.0"
  }
}

Response:
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "profileStatus": "Onboarding",
    "message": "OTP sent successfully to your phone"
  }
}
```

**What happens inside:**
1. Checks if profile exists (phone + country)
2. If NOT found in Profile Service → creates new profile
3. Creates ProfileDevice with device ID format: `trade-finance-ussd-{countryCode}-{phoneNumber}`
4. Sets profile status to BLOCKED with BlockReason=Onboarding
5. **🔍 IMSI/SIM Swap Check** - Verifies if SIM card was recently swapped
6. Generates OTP and sends via SMS/USSD
7. Records step: `OnboardingStepName.PhoneVerification` = INPROGRESS

**Key variables set:**
- `profileId` = new UUID
- `profile.pinSet` = FALSE
- `profile.blocked` = TRUE (BlockReason = Onboarding)
- `profile.profileTenantOrganizationId` = Trade Finance tenant ID

---

### **PHASE 1.5: IMSI/SIM Swap Security Check** 🔒

**Executed immediately after profile creation, before OTP is sent.**

#### What is SIM Swap Check?

SIM swap is a security threat where a bad actor:
1. Ports the customer's phone number to a new SIM card
2. Gains access to SMS-based authentication (OTP codes)
3. Can intercept messages meant for the real customer

**Trade Finance USSD must verify that the SIM card hasn't been swapped recently.**

#### How It Works (Internally)

```java
// Called in: ProfileCountryAndPhoneNoServiceImpl.verifyProfilePhoneNoDoNormalOnboarding()
// Line: 347

if (!isOnboardingComplete || isProfileBlockedByOnBoarding || 
    isBlockedByIMSISimSwapSecurityQuestionsChallenge || 
    isBlockedByIMSISimSwap) {
    
    imsiService.handleImsiChecks(
        savedProfile.getId(), 
        ImsiCheckDTO.builder().isFirstTimeLogin(false).build(), 
        tokenInfo.getClientId()
    );
}
```

**Service Chain:**
1. `ImsiService.handleImsiChecks()` - Entry point
2. `SimSwapRemoteService.getSimRegistrationDetails()` - Calls external IMSI provider
3. Checks: `daysBetween < simSwapConfiguration.getDays()` (default: 14 days)
4. Result: Profile may be BLOCKED with BlockReason="IMSISimSwap"

#### Possible Outcomes

| Outcome | Block Status | Next Step |
|---------|--------------|-----------|
| ✅ No swap detected (>14 days old) | Remains BLOCKED (Onboarding) | Continue to Phase 2: OTP verify |
| ⚠️ Swap detected (< 14 days old) | BLOCKED (IMSISimSwap) | Trigger security challenge (may require security questions) |
| ⚠️ Swap within 21 days | BLOCKED (IMSISimSwap) | Additional verification may be required |
| ❌ Service unavailable | Remains BLOCKED (Onboarding) | OTP still sent, but profile may remain blocked |

#### Configuration

**File:** `SimSwapConfiguration.java`

| Property | Default | Purpose |
|----------|---------|---------|
| `days` | 14 | Threshold for "recent swap" detection |
| `swapLimitDays` | 21 | Hard limit for swap window |
| `endpoint.verify` | (configured) | External IMSI service endpoint |
| `endpoint.challenge` | (configured) | Security challenge endpoint |
| `testImsiResponseType` | (optional) | Test mode (if enabled) |

#### ⚠️ **Does SIM Swap Require OTP Verification?**

**Short Answer: NO** - SIM Swap check does NOT require OTP verification.

**Detailed Outcomes:**

| Scenario | Block Status | OTP Needed? | Next Step |
|----------|--------------|------------|-----------|
| ✅ **No swap detected** | Remains BLOCKED (Onboarding) | ✅ YES (Phase 2) | Continue normally |
| ⚠️ **Swap < 14 days** | BLOCKED (SimSwap) | ❌ NO | User visits branch |
| ⚠️ **Swap 14-21 days + Has Security Q's** | BLOCKED (PendingSecurityQChallenge) | ❌ NO | Answer security questions first |
| ⚠️ **Swap 14-21 days + NO Security Q's** | BLOCKED (SimSwap) | ❌ NO | User visits branch |

**Why No OTP?**
- Code (line 373): `boolean isOtpVerificationRequired = false;`
- SimSwap uses security questions challenge (line 375), NOT OTP
- Trade Finance USSD skips security questions entirely
- Result: If swap detected → Profile blocked → User must visit branch

**Trade Finance Specific:**
- If SIM swap detected (any scenario)
- Profile gets BLOCKED with BlockReason=SimSwap
- User CANNOT proceed with onboarding
- User must visit branch for manual verification
- **No separate OTP is sent** for simSwap resolution

#### Code References

| Component | Location |
|-----------|----------|
| **Service** | `ImsiServiceImpl.java` |
| **Remote Call** | `SimSwapRemoteServiceImpl.java:31` - `getSimRegistrationDetails()` |
| **Check Call** | `ProfileCountryAndPhoneNoServiceImpl.java:347` |
| **OTP Check** | `ImsiServiceImpl.java:373` - `isOtpVerificationRequired = false` |
| **Challenge Creation** | `ImsiServiceImpl.java:381-390` - Security questions challenge |
| **Configuration** | `SimSwapConfiguration.java` |
| **DTO** | `ImsiCheckDTO`, `ImsiResponseDTO` |

---

### **PHASE 2: OTP Verification**

#### Endpoint 2: Verify OTP
```
POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}/otps/verify

Body:
{
  "otp": "123456",
  "purpose": "PhoneVerification"  // or "SelfResetPin"
}

Response:
{
  "data": {
    "message": "OTP verified successfully",
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd"
  }
}
```

**What happens inside:**
1. Validates OTP against stored OTP in NotificationOtp table
2. Checks NotificationOtp.purpose matches
3. Marks OTP as verified
4. Updates ForgotPinRequest status if applicable
5. Does NOT set profile as onboarded yet (still blocked)

**Important:** OTP verification is typically triggered during phone verification OR self-reset PIN flow.

---

### **PHASE 3: Pull Fiorano Core Banking Accounts** ⭐ KEY STEP

#### Endpoint 3: Link Customer Core Banking Accounts (Pull from Fiorano)
```
PATCH /v1/profiles/country/{countryId}/phone-no/{phoneNo}/accounts/link-customer-core-banking-accounts

Body:
{
  "id": "27466858"  // National ID OR Passport number
}

Response:
{
  "data": [
    {
      "accountId": "1234567890",
      "accountNumber": "0123456789",
      "accountType": "Savings",
      "accountStatus": "Active",
      "currency": "KES",
      "availableBalance": "50000.00"
    },
    {
      "accountId": "1234567891",
      "accountNumber": "0123456790",
      "accountType": "Checking",
      "accountStatus": "Active",
      "currency": "KES",
      "availableBalance": "100000.00"
    }
  ]
}
```

**What happens inside:**

1. **Get Profile** from database using country + phone
2. **Check if profile already has accounts** 
   - If YES and ID mismatch → return existing accounts (prevent re-linking)
   - If NO → proceed to pull from Fiorano

3. **Pull from Fiorano** (Core Banking System)
   ```java
   ProfileStoreOfValueListIdTypeAndProfile profileStoreOfValueListAndIdType = 
       profileStoreOfValueService.pullCoreBankingAccounts(
           profile,
           dto.getId(),           // National ID or Passport
           username,              // Phone number
           isThrowIfDuplicateEmailFound);
   ```

   **Inside `pullCoreBankingAccounts`:**
   - Calls `FioranoDataQueryService.getCustomerCoreBankingAccountsByIdNumberOrPassport()`
   - Query parameters: `idNumber` (tries first), then `passport`
   - Returns list of customer accounts from Flex-Cube (Fiorano system)
   - Maps Fiorano accounts to `ProfileStoreOfValue` entities
   - Saves to database

4. **Reload Profile** from database (after Fiorano pull in different transaction)
   ```java
   Profile savedProfile = profileRepository.findById(coreBankingProfile.getId()).orElseThrow(...)
   ```

5. **Check if Profile Onboarding is Complete**
   ```java
   boolean ifToUnblockOnBoardingProfile = profileStateCheckService.ifToUnblockOnBoardingProfile(
       savedProfile, 
       false,      // isSecurityQuestionsSetUpRequired = FALSE (skip security questions)
       true);      // isCheckSecurityQuestionsSetUp = TRUE
   ```

6. **If onboarding complete:**
   - Migrate credentials to auth service in background thread
   - Profile remains BLOCKED until PIN is set

**Critical Point:**
- Profile is NOT yet unblocked
- Profile is NOT yet onboarded
- Only Fiorano accounts have been pulled and linked
- Profile is still in BLOCKED state (BlockReason = Onboarding)

---

### **PHASE 4: PIN Setup**

#### Endpoint 4A: Generate Self-Reset PIN OTP (Automatic during Onboarding)
During Phase 1 (`verifyProfilePhoneNo`), the system ALSO:
```
// After phone verification OTP is sent, 
// ALSO generate Self-Reset PIN OTP

boolean isProfileDoingOnboarding = !isProfileAlreadyOnboarded && !isProfileResettingPinSelf;
if (isProfileDoingOnboarding) {
    notificationOtpService.generateSelfResetPinOtp(
        profile,
        true,                  // Is self-reset
        otpRequiredLength,     // USSD-specific OTP length
        isUSSDClient,          // TRUE for USSD
        !isUSSDClient,         // FALSE for USSD (no need for 2FA)
        isUSSDClient,          // TRUE for USSD
        ForgotPinRequestOrigin.SELF);
}

// This generates:
// 1. ProfileForgotPinRequest (for PIN setting)
// 2. NotificationOtp (with purpose = "SelfResetPin")
```

**⚠️ CLARIFICATION: OTP Count for Trade Finance USSD Onboarding**

✅ **Only ONE OTP is needed** for Trade Finance USSD onboarding:

| Phase | OTP Needed? | Purpose | Details |
|-------|-------------|---------|---------|
| **PHASE 1** | ✅ YES | Phone Verification | Sent during profile creation (line 459-480) |
| **PHASE 2** | ✅ YES (same OTP) | Verify Phone | Confirms the OTP from Phase 1 |
| **PHASE 3** | ❌ NO | Fiorano Linking | No OTP needed, just pull accounts |
| **PHASE 4** | ❌ NO | Set PIN | Reuses ForgotPinRequest from Phase 1, no separate OTP |

**Why Only One OTP?**
- Code: `isProfileDoingOnboarding` check (line 1131)
- If onboarding: sends ONE OTP with purpose `PhoneNumberVerification` (line 1149)
- ForgotPinRequest is created automatically during onboarding
- PIN setup uses this existing ForgotPinRequest, no separate OTP required
- This is different from "Reset PIN" flow which requires 2 OTPs

**Code Evidence:**
- **Phase 1 OTP Generation:** ProfileCountryAndPhoneNoServiceImpl.sendOTPToCustomer() (lines 458-480)
  - OTP Type: `PhoneNumberVerification`
  - Event: `CustomerPhoneVerificationOtp`
- **Phase 4 PIN Setup:** ProfileByIdServiceImpl.changePinViaForgotPin() (lines 792-1050)
  - Uses existing ForgotPinRequest (line 789)
  - No new OTP verification required
  - Sets PIN directly without Cellulant validation

---

#### Endpoint 4B: Set New PIN (Change PIN via Forgot PIN)
```
PATCH /v1/profiles/{profileId}/change-forgotten-pin

Body:
{
  "pin": "1234",
  "confirmPin": "1234",
  "deviceInfo": {
    "id": "trade-finance-ussd-KE-254711111111",
    "deviceName": "USSD Device",
    "deviceType": "USSD",
    "deviceOs": "USSD",
    "deviceOsVersion": "1.0",
    "deviceModel": "USSD",
    "buildVersion": "1.0"
  }
}

Response:
{
  "data": {
    "message": "PIN set successfully",
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "profileStatus": "Active"
  }
}
```

**What happens inside:**

1. **Validate Profile**
   - Check if profile exists
   - Check if profile is blocked by onboarding (allowed)
   - Check if profile is blocked by other reasons (NOT allowed)

2. **Get ForgotPinRequest**
   - Retrieves ProfileForgotPinRequest for this profile
   - If NULL and profile is onboarding → generate new one
   - If expired → regenerate and throw error asking to verify new OTP

3. **Validate OTP Verification**
   ```java
   if (forgotPinRequest.isOtpVerificationRequired() && !forgotPinRequest.isOtpVerified()) {
       throw new BadRequestException("Please verify OTP for this profile phone number");
   }
   ```

4. **Skip Security Questions** (for Trade Finance)
   ```java
   // Commented out for USSD/Trade Finance
   // if (forgotPinRequest.isSecurityQuestionsSetUpRequired() && !forgotPinRequest.isSecurityQuestionsSetUp()) {
   //     throw new BadRequestException("Please set up security questions");
   // }
   ```

5. **Validate PIN**
   - Check if `pin` equals `confirmPin`
   - Validate PIN against PIN rules (length, format, etc.)

6. **Validate Device**
   - Check if device belongs to profile or is new
   - If device mismatch with existing device → throw error

7. **Unblock Profile**
   ```java
   boolean isCheckSecurityQuestionsSetUp = 
       forgotPinRequest.isSecurityQuestionsSetUpRequired() || profile.isPinSet();
       
   boolean ifToUnblockOnBoardingProfile = profileStateCheckService.ifToUnblockOnBoardingProfile(
       profile,
       true,                           // isPinSet = TRUE
       isCheckSecurityQuestionsSetUp); // For USSD = FALSE (skip questions check)
       
   if (ifToUnblockOnBoardingProfile) {
       profile = profileEntityFactory.unblockOnBoardingProfile(profile);
   }
   ```

8. **Migrate PIN to Auth Service** (public-auth-service)
   ```java
   publicAuthDataSyncService.migratePinToAuthService(
       profile,
       dto.getPin(),
       dto.getConfirmPin(),
       false);
   ```

9. **Save Device** to profile
   ```java
   profileDeviceService.saveProfileDeviceData(
       profileDeviceAndMessage.getProfileDevice(),
       profile,
       profileId,
       deviceInfo,
       channelType);
   ```

10. **Update Onboarding Step**
    ```
    OnboardingStepName.PinSetUp = COMPLETED
    ```

11. **Profile Status Changes**
    - `profile.pinSet` = TRUE
    - `profile.blocked` = FALSE
    - `profile.blockReason` = NULL
    - Profile is now **ACTIVE and ONBOARDED**

---

## Endpoint Connection Map

```
FLOW DIAGRAM:

┌─────────────────────────────────────────────────────────────────┐
│ START: USSD User Dials Code                                     │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │ EP1: POST /profiles/country/ │
        │   /phone-no                  │
        │ - Create profile             │
        │ - Generate PhoneVerification  │
        │   OTP                        │
        │ - Generate SelfResetPin OTP  │
        │ - Block profile              │
        │ (status=Onboarding)          │
        └──────────┬───────────────────┘
                   │
                   ▼
        ┌──────────────────────────────┐
        │ EP2: POST /otps/verify       │
        │ - Verify Phone OTP           │
        │ - Unblock profile (?)        │
        └──────────┬───────────────────┘
                   │
                   ▼
        ┌──────────────────────────────┐
        │ EP3: PATCH /accounts/link-   │
        │      customer-core-banking-  │
        │      accounts                │
        │ - Pull from Fiorano          │
        │ - Link accounts to profile   │
        │ - Profile still blocked      │
        └──────────┬───────────────────┘
                   │
                   ▼
        ┌──────────────────────────────┐
        │ USSD App Prompts:            │
        │ "Enter Your PIN"             │
        │ + Validate SelfResetPin OTP  │
        └──────────┬───────────────────┘
                   │
                   ▼
        ┌──────────────────────────────┐
        │ EP4: PATCH /profiles/{id}/   │
        │      change-forgotten-pin    │
        │ - Verify OTP                 │
        │ - Set new PIN                │
        │ - Skip security questions    │
        │ - Unblock profile            │
        │ - Migrate PIN to auth        │
        │ - Profile = ACTIVE           │
        └──────────┬───────────────────┘
                   │
                   ▼
        ┌──────────────────────────────┐
        │ END: Profile Active &        │
        │      Onboarding Complete     │
        │ Can now transact             │
        └──────────────────────────────┘
```

---

## Cellulant PIN Validation

### Current Behavior (for DTB Mobile):
```
PATCH /v1/profiles/country/{countryId}/phone-no/{phoneNo}/validate-cellulant-pin

Endpoint: validateCellulantPin()
- Validates customer PIN against Cellulant service
- Only used if profile is not onboarded
- Part of normal onboarding flow for DTB Mobile
```

### Trade Finance USSD Behavior:
**SKIP Cellulant validation entirely** because:
1. Trade Finance pulls data from Fiorano, not Cellulant
2. PIN is set directly in Profile service (not verified against Cellulant)
3. No Cellulant integration needed

**How to implement:**
In `ProfileCountryAndPhoneNoServiceImpl.validateCellulantPin()`:
- Add check: `if (client.shouldSkipCellulantValidation() || client.isFioranoBasedClient())`
- Return success message without calling Cellulant service

---

## Device ID Generation During Onboarding

### Current System:
Device ID is provided by the CLIENT in the request:
```json
{
  "deviceInfo": {
    "id": "trade-finance-ussd-KE-254711111111",  // Client must provide this
    "deviceName": "USSD Device",
    ...
  }
}
```

### For Trade Finance USSD:
**Client (USSD frontend/middleware) must generate:**
```
DeviceID = "trade-finance-ussd-" + countryCode + "-" + phoneNumber

Examples:
- trade-finance-ussd-KE-254711111111
- trade-finance-ussd-UG-256701234567
- trade-finance-ussd-ZA-27821234567
```

### Device Registration Flow:
1. Client generates device ID in USSD format
2. Sends in `onboardingInfoDto.deviceInfo.id`
3. ProfileDeviceFactory creates ProfileDevice with this ID
4. Device is saved in `profile_device` table
5. Device status is set to ACTIVE (for USSD channel, new devices = ACTIVE)

---

## ProfileForgotPinRequest Lifecycle

```
State 1: NULL
- Profile is created
- No ForgotPinRequest yet

State 2: GENERATED (onboarding flow)
- Phone verification OTP sent
- SelfResetPin OTP generated
- ForgotPinRequest created with:
  - otpVerificationRequired = TRUE
  - otpVerified = FALSE
  - securityQuestionsSetUpRequired = FALSE (for Trade Finance)
  - securityQuestionsSetUp = FALSE (or skipped)
  - origin = ForgotPinRequestOrigin.SELF

State 3: OTP VERIFIED
- User verifies OTP successfully
- ForgotPinRequest.otpVerified = TRUE
- Ready for PIN setting

State 4: PIN SET
- User calls changePinViaForgotPin
- PIN is set
- Profile is unblocked
- ForgotPinRequest can be deleted or marked as completed
```

---

## Trade Finance USSD Onboarding Configuration

### Client Configuration Flags:
```java
ClientDetails.isRequireOnboardingProfileHaveFlexAccounts = FALSE
ClientDetails.isAnAstraTenant = FALSE
ClientDetails.isAstraOnboardingRequireSelfieScore = FALSE
ClientDetails.isCompleteProfileOnboardingAfterPhoneVerification = FALSE (or TRUE if auto-complete)
```

### Tenant Configuration:
```yaml
tenants-configurations:
  trade-finance:
    id: "trade-finance-tenant-uuid"
    name: "Trade Finance"
    # Minimal config - can add more if needed later
```

### Onboarding Step Requirements:
```
SKIP:
- SecurityQuestionsSetUp
- LivenessCheck
- AstraWalletCreation
- KraPinUpload
- KraPinOcr
- IprsCheck
- IdDocumentUpload

INCLUDE:
- PhoneVerification
- AccountOpening (linked from Fiorano)
- PinSetUp
```

---

## Connection Summary: How the Endpoints Work Together

| Step | Endpoint | Purpose | Profile State Before | Profile State After |
|------|----------|---------|----------------------|---------------------|
| 1 | POST `/profiles/country/{id}/phone-no/{phone}` | Create profile, verify phone | N/A | BLOCKED (Onboarding) |
| 2 | POST `/otps/verify` | Verify phone OTP | BLOCKED | BLOCKED |
| 3 | PATCH `/accounts/link-customer-core-banking-accounts` | Pull Fiorano accounts | BLOCKED | BLOCKED |
| 4 | PATCH `/profiles/{id}/change-forgotten-pin` | Set PIN | BLOCKED | **ACTIVE** |

**Key Connection:**
- Endpoint 3 (link accounts) does NOT unblock profile
- Endpoint 4 (set PIN) unblocks profile IF no security questions required
- For Trade Finance: Security questions check is skipped → PIN setting = onboarding complete

---

## For Trade Finance Implementation

### Required Changes:

1. **Create Trade Finance Tenant**
   ```sql
   INSERT INTO tenant_organization (id, name) VALUES ('trade-finance-uuid', 'Trade Finance')
   ```

2. **Create Trade Finance USSD Client**
   ```sql
   INSERT INTO clients (id, name, client_type, channel_type, profile_tenant_organization_id, ...) 
   VALUES ('trade-finance-ussd-uuid', 'Trade Finance USSD', ..., 'trade-finance-uuid', ...)
   ```

3. **Set Client Configuration Flags**
   - `isRequireOnboardingProfileHaveFlexAccounts = FALSE`
   - `isAnAstraTenant = FALSE`

4. **Device ID Format Handler** (OPTIONAL, if not client-managed)
   ```java
   // In ProfileDeviceFactory or ProfileDeviceService
   if (client.isTradeFinanceUssd() || client.isFioranoBasedClient()) {
       if (deviceId == null || deviceId.isEmpty()) {
           deviceId = String.format("trade-finance-ussd-%s-%s", countryCode, phoneNumber);
       }
   }
   ```

5. **Skip Cellulant Validation**
   ```java
   // In ProfileCountryAndPhoneNoServiceImpl
   if (client.isFioranoBasedClient()) {
       return new MessageResponseDto("PIN validation skipped for Fiorano-based profiles");
   }
   ```

6. **Skip Security Questions**
   - Endpoint 4 (`changePinViaForgotPin`) already has logic for this
   - Controlled by `forgotPinRequest.isSecurityQuestionsSetUpRequired()`
   - Set FALSE in ForgotPinRequest generation for Trade Finance

---

## Testing Scenarios

### Happy Path (Complete Onboarding):
```
1. POST /v1/profiles/country/KE/phone-no/254711111111
   → OTP sent
   
2. POST /v1/profiles/country/KE/phone-no/254711111111/otps/verify
   → OTP verified
   
3. PATCH /v1/profiles/country/KE/phone-no/254711111111/accounts/link-customer-core-banking-accounts
   Body: {"id": "27466858"}
   → Accounts linked (2-3 accounts returned)
   
4. PATCH /v1/profiles/{profileId}/change-forgotten-pin
   Body: {"pin": "1234", "confirmPin": "1234", "deviceInfo": {...}}
   → PIN set, profile unblocked, onboarding complete
```

### Edge Cases:
- Profile already exists (duplicate phone detection)
- ID number not found in Fiorano (no accounts returned)
- OTP expired during onboarding
- Device mismatch when setting PIN
- PIN format validation failures

---

## Summary

**For Trade Finance USSD onboarding:**

1. **Phone Verification** (EP1): Creates profile, blocks it, sends OTP
2. **OTP Verification** (EP2): Verifies customer's phone number
3. **Account Linking** (EP3): Pulls from Fiorano using National ID/Passport, links accounts
4. **PIN Setup** (EP4): Sets new PIN, unblocks profile, completes onboarding

**No Cellulant validation, No Security Questions, No Astra integration needed.**

The endpoints are sequential and interdependent - profile state transitions through each endpoint until finally unblocked at PIN set.

---

## Complete Endpoint Reference Summary

### All Endpoints in Order (with HTTP Methods)

| Phase | Endpoint | HTTP Method | Purpose | Status Code | Code Location |
|-------|----------|-------------|---------|-------------|----------------|
| **PRE** | `/v1/profiles/country/{countryId}/phone-no/{phoneNo}` | **GET** | Check if profile exists | 200/404 | ProfileCountryAndPhoneNoV1Controller:155 |
| **1** | `/v1/profiles/country/{countryId}/phone-no/{phoneNo}` | **POST** | Create new profile + verify phone | 201 | ProfileCountryAndPhoneNoV1Controller:56 |
| **1.5** | *(Internal Service)* | *(N/A)* | **🔒 IMSI/SIM Swap Check** | *(N/A)* | ImsiServiceImpl.handleImsiChecks() |
| **2** | `/v1/profiles/country/{countryId}/phone-no/{phoneNo}/otps/verify` | **POST** | Verify OTP | 200 | OtpVerificationV1Controller |
| **3** | `/v1/profiles/country/{countryId}/phone-no/{phoneNo}/accounts/link-customer-core-banking-accounts` | **PATCH** | Pull Fiorano accounts + link | 200 | ProfileCountryAndPhoneNoV1Controller:200+ |
| **4** | `/v1/profiles/{profileId}/change-forgotten-pin` | **PATCH** | Set new PIN + unblock profile | 200 | ProfileByIdV1Controller |

### Critical Flow Diagram

```
User dials USSD
    ↓
[PRE-PHASE] GET /v1/profiles/country/{countryId}/phone-no/{phoneNo}
    ↓
    ├─ 200 OK + isBlocked=false
    │  └─ User already onboarded → Show main menu, SKIP onboarding
    │
    ├─ 200 OK + isBlocked=true + blockReason="Onboarding"
    │  └─ User interrupted onboarding → Resume from onboardingStepProgress
    │
    └─ 404 NOT FOUND
       └─ [PHASE 1] POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}
          └─ Create profile + send phone verification OTP
          └─ Response: 201 CREATED, profileId, isBlocked=true
          
          └─ [PHASE 1.5] 🔒 IMSI/SIM Swap Check
             └─ Verify SIM wasn't swapped recently (<14 days)
             └─ If swapped: May require security challenge
             └─ If clear: Proceed to OTP verification
             
             └─ [PHASE 2] POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}/otps/verify
                └─ Verify phone OTP
                └─ Response: 200 OK
                
                └─ [PHASE 3] PATCH /v1/profiles/country/{countryId}/phone-no/{phoneNo}/accounts/link-customer-core-banking-accounts
                   └─ Pull data from Fiorano using National ID/Passport
                   └─ Response: 200 OK + list of accounts
                   └─ Profile still BLOCKED (Onboarding)
                   
                   └─ [PHASE 4] PATCH /v1/profiles/{profileId}/change-forgotten-pin
                      └─ Set new PIN (Security Questions SKIPPED)
                      └─ Response: 200 OK
                      └─ Profile UNBLOCKED ✓
                      └─ Onboarding COMPLETE ✓
```

### Key Implementation Points

**GET Request (Pre-Phase):**
- **Always call first** - Read-only, no side effects
- Returns ProfileBlockInfoDTO with:
  - `profileId` - Use for later endpoints
  - `isBlocked` - Current block status
  - `blockReason` - Why blocked (e.g., "Onboarding")
  - `onboardingStepProgress` - Which step to resume from
  - `pinSet` - Whether PIN is already set

**POST Request (Phase 1):**
- **Only call if GET returns 404**
- Must include device info with ID format: `trade-finance-ussd-{countryCode}-{phoneNumber}`
- Creates new profile in BLOCKED state
- Sends phone verification OTP

**PATCH Request (Phase 3):**
- Input: National ID or Passport (in StringIdDto)
- Triggers Fiorano pull via FioranoDataQueryService
- Profile remains BLOCKED

**PATCH Request (Phase 4):**
- Completes onboarding by setting PIN
- Unblocks profile
- No security questions required
- No Cellulant validation

---

## Flow Behavior Matrix

| Scenario | GET Result | Action | Next Step |
|----------|-----------|--------|-----------|
| New user first time | 404 | POST to create | Phase 1 → 2 → 3 → 4 |
| User completed onboarding | 200, isBlocked=false | Skip onboarding | Show main menu |
| User interrupted at phone verify | 200, isBlocked=true, step=PhoneVerification | Resume | Resend OTP, then Phase 2 → 3 → 4 |
| User interrupted at PIN setup | 200, isBlocked=true, step=PinSetUp | Resume | Go to PIN setup Phase 4 |

---

## Testing Scenarios

### Scenario 1: Complete New User Onboarding
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   → 404 (not found)
   
2. POST /v1/profiles/country/KE/phone-no/254711111111
   → 201 (created), profileId, isBlocked=true
   
3. POST /v1/profiles/country/KE/phone-no/254711111111/otps/verify
   → 200 (verified)
   
4. PATCH /v1/profiles/country/KE/phone-no/254711111111/accounts/link-customer-core-banking-accounts
   → 200 (accounts linked), profile still blocked
   
5. PATCH /v1/profiles/{profileId}/change-forgotten-pin
   → 200 (PIN set), profile unblocked ✓
```

### Scenario 2: User Resuming Interrupted Onboarding
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   → 200 (found), isBlocked=true, step=PinSetUp
   → Resume from PIN setup
   
2. PATCH /v1/profiles/{profileId}/change-forgotten-pin
   → 200 (PIN set), profile unblocked ✓
```

### Scenario 3: Already Onboarded User
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   → 200 (found), isBlocked=false, pinSet=true
   → User is fully onboarded, proceed to main menu ✓
```

---

## Controller & Service Code References

**Profile Existence Check (GET):**
- Controller: ProfileCountryAndPhoneNoV1Controller.java:155
- Service: ProfileCountryAndPhoneNoServiceImpl.java:686-716
- Returns: ProfileBlockInfoDTO

**Profile Creation (POST):**
- Controller: ProfileCountryAndPhoneNoV1Controller.java:56
- Service Method: ProfileCountryAndPhoneNoServiceImpl.verifyProfilePhoneNo()
- Device ID created in this step

**Fiorano Account Linking (PATCH):**
- Service Method: ProfileCountryAndPhoneNoServiceImpl.lookUpCustomer() (lines 1162-1209)
- Calls: FioranoDataQueryService.getCustomerCoreBankingAccountsByIdNumberOrPassport()
- Saves accounts as ProfileStoreOfValue entities

**PIN Setup (PATCH):**
- Controller: ProfileByIdV1Controller
- Service Method: ProfileByIdServiceImpl.changePinViaForgotPin() (lines 792-1050)
- Unblocks profile at line 879 with forgotPinRequest.isSecurityQuestionsSetUpRequired = FALSE
