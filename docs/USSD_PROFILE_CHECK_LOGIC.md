# USSD Profile Check Logic: GET vs POST

## Quick Answer

**Two Different Endpoints, Two Different Purposes:**

```
┌─────────────────────────────────────────────────────────────┐
│ USER DIALS USSD CODE                                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌──────────────────────────────┐
        │ FIRST STEP: CHECK if         │
        │ profile exists               │
        └──────────────────────────────┘
        ⚠️ Use: GET /v1/profiles/country/{countryId}/phone-no/{phoneNo}
           NO body needed
           Returns: Profile exists? Onboarded? Blocked?
                    
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
   ✅ Profile Exists      ❌ Profile NOT Found
   & Onboarded               (ResourceNotFoundException)
         │                       │
         ├─ Active          ├─ Create New Profile
         ├─ Ready to use    └─ Start Onboarding
         └─ Show Menu              │
                                   ▼
                     ┌──────────────────────────────┐
                     │ SECOND STEP: CREATE &        │
                     │ START ONBOARDING             │
                     └──────────────────────────────┘
                     ⚠️ Use: POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}
                        With onboarding body
                        Returns: New profile created, OTP sent
```

---

## HTTP Methods: GET vs POST

### **GET** `/v1/profiles/country/{countryId}/phone-no/{phoneNo}`

```http
GET /v1/profiles/country/KE/phone-no/254711111111

Response Status: 200 OK

Body: NONE (Read-only, no data modification)

Response:
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "firstName": "John",
    "phoneNumber": "254711111111",
    "status": "Active",
    "blocked": false,
    "blockReason": null,
    "blockReasonDescription": "Profile is active",
    "onboardingStepProgress": {
      "currentStep": "PinSetUp",
      "status": "COMPLETED"
    }
  }
}

OR (Profile doesn't exist):

Response Status: 404 NOT FOUND

Body:
{
  "error": "Profile with username not found"
}
```

**Purpose:** Read-only profile status check
**No Side Effects:** Doesn't create anything, doesn't modify anything
**Used For:** USSD check-in logic

---

### **POST** `/v1/profiles/country/{countryId}/phone-no/{phoneNo}`

```http
POST /v1/profiles/country/KE/phone-no/254711111111
Content-Type: application/json

Body:
{
  "isExistingCustomer": false,
  "isAcceptTermsAndConditions": true,
  "isAcceptMarketingConsent": true,
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

Response Status: 201 CREATED

Body:
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "profileStatus": "Onboarding",
    "blocked": true,
    "blockReason": "Onboarding",
    "message": "OTP sent to your phone. Please verify to continue."
  }
}
```

**Purpose:** Create new profile + start onboarding
**Side Effects:** 
- Creates Profile entity
- Creates ProfileDevice
- Blocks profile (BlockReason=Onboarding)
- Generates & sends OTP
- Records onboarding step

---

## Why Check Profile First (GET)?

### **The Problem:**

If you POST directly without checking:

```
Scenario 1: Profile exists, fully onboarded
  POST /v1/profiles/country/KE/phone-no/254711111111
  
  Result: ❌ DUPLICATE_RESOURCE exception
           "Profile with this phone number already exists"
           User gets confused!

Scenario 2: Profile exists, in middle of onboarding
  POST /v1/profiles/country/KE/phone-no/254711111111
  
  Result: ❌ FORBIDDEN exception
           "Cannot create profile: user already exists"
           Loses onboarding progress!

Scenario 3: Profile doesn't exist
  POST /v1/profiles/country/KE/phone-no/254711111111
  
  Result: ✅ Creates new profile
           But you wasted a DB transaction check
```

### **The Solution: Check First (GET)**

```
Scenario 1: Profile exists, fully onboarded
  GET /v1/profiles/country/KE/phone-no/254711111111
  
  Response: ✅ 200 OK with profile details
            blocked = false
            status = Active
  
  Action: Skip onboarding, show main menu

Scenario 2: Profile exists, in middle of onboarding  
  GET /v1/profiles/country/KE/phone-no/254711111111
  
  Response: ✅ 200 OK with profile details
            blocked = true
            blockReason = "Onboarding"
            currentStep = "PhoneVerification"
  
  Action: Resume onboarding from step 2 (OTP verification)

Scenario 3: Profile doesn't exist
  GET /v1/profiles/country/KE/phone-no/254711111111
  
  Response: ❌ 404 NOT FOUND
            "Profile with username not found"
  
  Action: Call POST to create new profile
          Start fresh onboarding
```

---

## Trade Finance USSD Decision Flow

### **Step 1: Check Profile (GET)**

```java
// USSD App logic (pseudo-code):

String countryCode = "KE";
String phoneNumber = "254711111111";

try {
    // GET request - Check if profile exists
    ProfileCheckResponse profile = GET(
        "/v1/profiles/country/{countryCode}/phone-no/{phoneNumber}",
        countryCode,
        phoneNumber
    );
    
    // ✅ Profile found
    if (!profile.isBlocked()) {
        // Profile is active and onboarded
        showMainMenu(profile);  // Show account menu, transactions, etc.
    } else if (profile.getBlockReason().equals("Onboarding")) {
        // Profile exists but incomplete onboarding
        String currentStep = profile.getOnboardingStepProgress().getCurrentStep();
        resumeOnboarding(profile, currentStep);
    } else {
        // Profile is blocked for other reasons (fraud, closed, etc.)
        showBlockedMessage(profile.getBlockReasonDescription());
    }
    
} catch (ResourceNotFoundException e) {
    // ❌ Profile does not exist
    startNewOnboarding();  // Proceed to POST to create new profile
}
```

### **Step 2: If Profile Not Found, Create New (POST)**

```java
// Called only if GET returned 404

try {
    ProfileCreationResponse response = POST(
        "/v1/profiles/country/{countryCode}/phone-no/{phoneNumber}",
        {
            isExistingCustomer: false,
            isAcceptTermsAndConditions: true,
            isAcceptMarketingConsent: true,
            deviceInfo: {
                id: "trade-finance-ussd-KE-254711111111",
                deviceName: "USSD Device",
                ...
            }
        }
    );
    
    // ✅ New profile created, OTP sent
    profileId = response.getProfileId();
    showMessage("OTP sent to your phone. Enter OTP to verify.");
    promptForOTP();  // Next step: OTP verification
    
} catch (DuplicateResourceException e) {
    // This shouldn't happen if GET was successful
    // Retry GET to sync state
    log.error("Unexpected duplicate profile", e);
}
```

---

## Code Implementation (GET Method)

### **Controller: `ProfileCountryAndPhoneNoV1Controller.java`**

```java
@GetMapping  // Line 155
@ResponseStatus(HttpStatus.OK)
@Operation(summary = "Get a profile by username")
public ApiResponseDto<ProfileBlockInfoDTO> getProfileByUsername(
        @PathVariable("countryId") @NotBlank String countryId,
        @PathVariable("phoneNo") @NotBlank String phoneNo,
        Authentication authentication
) throws ResourceNotFoundException {
    ProfileBlockInfoDTO profileBlockInfoView = profileCountryAndPhoneNoService
        .getProfileByUsername(countryId, phoneNo, authentication);
    
    return ApiResponseDto.fromHttpStatusAndResponse(
        HttpStatus.OK,
        profileBlockInfoView,
        false,
        profileBlockInfoView.getBlockReasonDescription(),
        true,
        profileBlockInfoView.isBlocked() ? 
            ApiResponseStatus.PROFILE_BLOCKED.getResponseCode() : null);
}
```

### **Service: `ProfileCountryAndPhoneNoServiceImpl.java`**

```java
@Override
public ProfileBlockInfoDTO getProfileByUsername(
        String countryId, 
        String username, 
        Authentication authentication) 
    throws ResourceNotFoundException {
    
    // 1. Get TokenInfo to know which tenant this request is for
    TokenInfo tokenInfo = authenticationHelperService.getTokenInfo(authentication);
    
    // 2. Query database - find profile by phone + country + tenant
    Profile profile = profileRepository
        .findByPhoneNumberAndProfileTenantOrganizationIdAndCountryIdOrCountryCallingCode(
            username,                                    // phone number
            tokenInfo.getProfileTenantOrganizationId(),  // tenant ID
            countryId,                                   // country code (e.g., "KE")
            countryId,                                   // also try as calling code
            Profile.class
        )
        .orElseThrow(() -> 
            // ❌ Profile not found - throw 404
            new ResourceNotFoundException(
                String.format("Profile with username %s not found", username)
            )
        );
    
    // 3. Check if profile is in onboarding (still incomplete)
    boolean isProfilePerformingOnboarding = 
        profileStateCheckService.isProfileBlockedByOnBoarding(profile) ||
        !profileStateCheckService.isOnboardingComplete(profile);
    
    ProfileOnboardingStepProgress profileOnboardingStepProgress = null;
    
    if (isProfilePerformingOnboarding && profile.getId() != null) {
        // If profile is incomplete, get current step so client knows where to resume
        profileOnboardingStepProgress = profileOnboardingStepsService
            .getCurrentStepForClient(profile.getId(), tokenInfo.getClientId());
    }
    
    // 4. Map to response DTO and return
    return profileMapper.profileAndProfileOnboardingStepProgressToProfileBlockInfoDTO(
        profile,
        profileOnboardingStepProgress,
        null
    );
}
```

### **Database Query:**

```sql
-- Simplified version of what the repository does:

SELECT * FROM profile
WHERE 
    phone_number = '254711111111'  -- Input phone number
    AND profile_tenant_organization_id = 'trade-finance-uuid'  -- Tenant filter
    AND (country_id = 'KE' OR country_calling_code = 'KE')  -- Country filter
LIMIT 1;

-- Result:
-- ✅ If found: Returns profile with status, blocked flag, etc.
-- ❌ If not found: Returns empty, which triggers ResourceNotFoundException
```

---

## Response Details: What GET Returns

### **For Active, Onboarded Profile:**

```json
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "254711111111",
    "country": "Kenya",
    "status": "Active",
    "blocked": false,
    "blockReason": null,
    "blockReasonDescription": "Profile is active",
    "onboardingStepProgress": null,  // Onboarding complete
    "pinSet": true,
    "profileTenantOrganizationId": "trade-finance-uuid"
  },
  "responseCode": null,
  "message": null
}
```

**What to do:** Skip onboarding, show main menu

---

### **For Profile in Middle of Onboarding:**

```json
{
  "data": {
    "profileId": "584877a7-be2d-42f5-b658-1e24b20b17cd",
    "firstName": null,  // Not yet filled
    "lastName": null,
    "phoneNumber": "254711111111",
    "country": "Kenya",
    "status": "Onboarding",
    "blocked": true,
    "blockReason": "Onboarding",
    "blockReasonDescription": "Onboarding not complete. Please proceed to onboarding.",
    "onboardingStepProgress": {
      "stepName": "PhoneVerification",
      "stepId": "PhoneVerification",
      "status": "INPROGRESS"
    },
    "pinSet": false,
    "profileTenantOrganizationId": "trade-finance-uuid"
  },
  "responseCode": "PROFILE_BLOCKED",
  "message": "Profile is blocked during onboarding"
}
```

**What to do:** Resume from step "PhoneVerification" (send new OTP or verify existing)

---

### **Profile Not Found (404):**

```json
{
  "error": "ResourceNotFoundException",
  "message": "Profile with username 254711111111 not found",
  "statusCode": 404
}
```

**What to do:** Call POST to create new profile

---

## Trade Finance USSD Implementation Pseudo-Code

```typescript
// USSD App Entry Point
async function handleUSSDRequest(countryCode: string, phoneNumber: string) {
  
  // STEP 1: CHECK if profile exists (GET)
  try {
    const existingProfile = await getProfile(countryCode, phoneNumber);
    
    // ✅ Profile exists
    if (!existingProfile.blocked) {
      // Profile is active and onboarded
      return showMainMenu(existingProfile);
    }
    
    if (existingProfile.blockReason === 'Onboarding') {
      // Profile exists but incomplete
      const currentStep = existingProfile.onboardingStepProgress?.stepName;
      
      switch (currentStep) {
        case 'PhoneVerification':
          return promptForOTP();  // Resume OTP verification
        case 'PinSetUp':
          return promptForPinSetup();  // Resume PIN setup
        default:
          return showOnboardingMenu();
      }
    }
    
    if (existingProfile.blocked) {
      // Blocked for other reasons (fraud, suspended, etc.)
      return showMessage(existingProfile.blockReasonDescription);
    }
    
  } catch (error) {
    if (error.statusCode === 404) {
      // ❌ Profile does not exist
      return startNewOnboarding();
    }
    
    // Other errors
    return showErrorMessage(error.message);
  }
}

// STEP 2: Start new onboarding (POST)
async function startNewOnboarding() {
  const response = await createProfile({
    countryCode,
    phoneNumber,
    onboardingData: {
      isExistingCustomer: false,
      isAcceptTermsAndConditions: true,
      isAcceptMarketingConsent: true,
      deviceInfo: {
        id: `trade-finance-ussd-${countryCode}-${phoneNumber}`,
        deviceName: 'USSD Device',
        deviceType: 'USSD',
        deviceOs: 'USSD',
        deviceOsVersion: '1.0',
        deviceModel: 'USSD',
        buildVersion: '1.0'
      }
    }
  });
  
  // ✅ Profile created, OTP sent
  showMessage('OTP sent. Please enter it to verify your phone number.');
  promptForOTP();
}
```

---

## Summary Table

| Aspect | GET | POST |
|--------|-----|------|
| **Purpose** | Check profile existence & status | Create new profile + start onboarding |
| **HTTP Method** | GET | POST |
| **Body Required** | NO | YES (onboarding data) |
| **Side Effects** | None (read-only) | Creates profile, blocks it, sends OTP |
| **Success Response** | 200 OK | 201 CREATED |
| **Profile Not Found** | 404 NOT FOUND | Would error (but you called GET first) |
| **Profile Exists, Onboarded** | 200 OK + blocked=false | 409 CONFLICT / 403 FORBIDDEN |
| **Use Case** | "Is user registered?" | "Register new user" |
| **Called When** | USSD session starts | After GET returns 404 |

---

## Best Practices for Trade Finance USSD

### ✅ DO:
1. **Always call GET first** - no exceptions
2. **Handle 404 gracefully** - it's expected for new users
3. **Check profile.blocked flag** - decide between menu vs resume/error
4. **Check onboardingStepProgress** - know exactly where to resume
5. **Log API calls** - for debugging interrupted sessions
6. **Cache profile status** - short TTL (5 min) to reduce DB hits

### ❌ DON'T:
1. **Call POST directly without GET** - will fail for existing users
2. **Assume profile exists** - causes 404 errors
3. **Ignore blockReason** - different reasons need different UX
4. **Hardcode onboarding steps** - fetch current step from DB
5. **Retry POST on duplicate** - instead, call GET to sync state

---

## Error Handling Examples

```java
// USSD Controller
@PostMapping("/check-profile")
public ResponseEntity<?> checkProfile(
        @RequestParam String countryCode,
        @RequestParam String phoneNumber,
        Authentication auth) {
    
    try {
        ProfileBlockInfoDTO profile = profileService.getProfileByUsername(
            countryCode, 
            phoneNumber, 
            auth
        );
        
        // ✅ Profile exists
        if (!profile.isBlocked()) {
            return ResponseEntity.ok(new USSDMenuResponse("existing_customer_menu"));
        }
        
        if (profile.isBlocked() && profile.getBlockReason().equals("Onboarding")) {
            return ResponseEntity.ok(new USSDResumeResponse(
                profile.getOnboardingStepProgress().getCurrentStep()
            ));
        }
        
        return ResponseEntity.status(403).body(
            new USSDErrorResponse(profile.getBlockReasonDescription())
        );
        
    } catch (ResourceNotFoundException e) {
        // ❌ Profile not found - this is OK for new users
        return ResponseEntity.status(404).body(
            new USSDNewUserResponse("start_onboarding")
        );
    }
}
```

---

This is why you need BOTH GET (to check) and POST (to create) in your Trade Finance USSD implementation!
