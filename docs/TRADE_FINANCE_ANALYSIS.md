# Trade Finance USSD Onboarding - Analysis & Design Breakdown

## Executive Summary
This document outlines the current architecture and identifies changes needed to support a new **Trade Finance** tenant organization with a **Trade Finance USSD** client that has a simplified onboarding flow (no security questions, no Cellulant validation).

---

## 1. CURRENT ARCHITECTURE OVERVIEW

### 1.1 Services Involved
- **Public Auth Service** - Manages tenant organizations, clients, auth users, and authentication
- **Profile Service** - Manages customer profiles, onboarding workflows, and integrations with external systems (Fiorano, Cellulant, M247)

### 1.2 Core Entities & Models

#### Public Auth Service:
- **TenantOrganization** - Represents the tenant organization (e.g., "Eatta")
- **ClientDetails** - Represents a client under a tenant (e.g., "Eatta Mobile", "Eatta Web")
- **ClientType** enum - Current values: Client, Partner, Buyer, Broker, Producer, Warehouse, Factory, Insurer, Merchant
- **AuthUser** - Authentication users linked to tenants/clients

#### Profile Service:
- **Profile** - Customer profile tied to country + phone number
- **OnboardingType** enum - Current values: New, Existing
- **OnboardingStepName** enum - Steps: PhoneVerification, StaticDataEntry, IprsCheck, IdDocumentUpload, IdDocumentOcrFront, IdDocumentOcrBack, KraPinUpload, KraPinOcr, LivenessCheck, AstraWalletCreation, AccountOpening, **PinSetUp**, **SecurityQuestionsSetUp**
- **ProfileDevice** - Device linked to a profile with deviceId format

---

## 2. CURRENT ONBOARDING FLOW (Standard - e.g., Eatta)

### Flow Overview
```
User Dial USSD Code
    ↓
ProfileCountryAndPhoneNoV1Controller - verifyProfilePhoneNo()
    ↓
Check Profile by Country + Phone Number in Profile Service
    ├─→ Profile EXISTS: 
    │   └─→ Continue with existing profile
    │
    └─→ Profile NOT FOUND:
        ├─→ Attempt to pull from M247 (check linked accounts)
        ├─→ If no M247, pull from Fiorano Core Banking (via account number lookup)
        └─→ Create new profile in Profile Service
    ↓
OnBoarding Steps (Conditional based on client):
    1. Phone Verification (OTP)
    2. Static Data Entry
    3. IPRS Check
    4. ID Document Upload + OCR (Front + Back)
    5. KRA PIN Upload + OCR
    6. Liveness Check
    7. Astra Wallet Creation (if applicable)
    8. Account Opening
    9. PIN Setup (Mandatory)
    10. Security Questions Setup (Mandatory) ← **SKIPPED FOR TRADE FINANCE**
    11. Cellulant PIN Validation (Implicit) ← **SKIPPED FOR TRADE FINANCE**
```

### Current Key Endpoints

#### Public Auth Service:
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/v1/tenant-organizations` | GET | List all tenant organizations |
| `/v1/auth-users` | POST | Create auth user |
| `/v1/auth-users` | GET | List auth users |
| `/v1/auth-users/validate-user` | POST | Validate user credentials |

#### Profile Service:
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/v1/profiles/country/{countryId}/phone-no/{phoneNo}` | POST | Create/verify profile (Entry point) |
| `/v1/profiles/{profileId}/on-boardings/client/{clientId}` | GET | Get onboarding status for client |
| `/v1/profiles/look-up/account-number/{accountNo}` | GET | Lookup profile from Core Banking |
| `/v1/profiles/country/{countryId}/phone-no/{phoneNo}/devices/{deviceId}/...` | POST/GET | Device-specific operations |

---

## 3. FIORANO CORE BANKING INTEGRATION (Flex-Cube)

### Current Implementation Details

#### How Data is Pulled from Fiorano:
```java
// FioranoDataQueryServiceImpl.java
- getCoreBankingAccountsByIdNumber(String id) 
  - Uses FieldDefinitionUtils.ID_NO
  - Queries Fiorano with national ID
  
- getCoreBankingAccountsByPassport(String id)
  - Uses FieldDefinitionUtils.PASSPORT
  - Queries Fiorano with passport number
  
- getCustomerCoreBankingAccountsByIdNumberOrPassport(String idNumberOrPassport)
  - Tries national ID first, then passport
  - Falls back if first attempt fails
```

#### Fiorano Query Process:
1. **Input**: National ID or Passport number (+ country code, phone number)
2. **Request Type**: FioranoCustomerAccountsRequestDto
3. **Fields**: 
   - idType: "ID_NO" or "PASSPORT"
   - idValue: The ID/Passport value
4. **Response**: FioranoCustomerAccountStatusResponse containing customer details and accounts
5. **Mapping**: Customer data mapped to Profile entity

#### Current Fiorano Service Methods:
```java
- getCustomerAccountsByIdOrPassport(FioranoCustomerAccountsRequestDto)
  - Primary method for fetching customer accounts by ID/Passport
  
- queryCustomerByCIF(String customerNo)
  - Query by Customer ID (CIF)
  
- queryCustomerAccountsByAccountNumber(String accountNo)
  - Query by bank account number
```

#### Configuration:
- **Base URL**: Configured in `FioranoConfiguration.java`
- **Endpoints**: Query customer, query accounts, query CIF details, etc.
- **Authentication**: Token-based (fetchFioranoToken)

---

## 4. CELLULANT INTEGRATION (Current)

### Current Implementation:
```java
// CellulantConfiguration.java
- cellulantBaseUrl: HTTP endpoint
- cellulantBasicAuthPassword: Credentials
- cellulantBasicAuthUsername: Credentials

// Endpoints:
- cellulantAuthenticate: Used for PIN validation during onboarding
```

### Current Usage:
- PIN validation after user sets new PIN
- Implicit in onboarding flow (validates PIN against Cellulant system)
- **For Trade Finance USSD**: This validation should be **SKIPPED**

---

## 5. SECURITY QUESTIONS (Current)

### Current Implementation:
```java
// OnboardingStepName enum includes:
- SecurityQuestionsSetUp

// Flow:
1. User completes PIN setup
2. System prompts for security questions
3. User selects questions and provides answers
4. Answers stored in profile
```

### For Trade Finance USSD:
- **This step should be SKIPPED entirely**
- No security questions prompted
- No security question answers stored

---

## 6. DEVICE ID FORMAT (Current)

### Current Device ID Pattern:
```
Format varies by client/channel:
- Mobile: mobile-{identifier}
- Web: web-{identifier}
- USSD (other clients): ussd-{identifier}

For Trade Finance USSD:
- Pattern: trade-finance-ussd-{countryCode}-{phoneNumber}
- Example: trade-finance-ussd-KE-254711111111
```

### Device Management:
- Stored in `ProfileDevice` entity
- Linked to Profile + clientId
- Used for device-specific security/biometric challenges

---

## 7. WHAT'S CURRENTLY THERE (Existing Implementation)

### In Public Auth Service:

#### Tenant Organization Management:
- ✅ `TenantOrganization` model with name, code, status
- ✅ `TenantOrganizationRepository` for persistence
- ✅ `TenantOrganizationService` for CRUD operations
- ✅ Endpoint: `/v1/tenant-organizations` (GET all)

#### Client Management:
- ✅ `ClientDetails` model with tenant link
- ✅ `ClientType` enum (limited to specific types)
- ✅ `ClientRepository` for persistence
- ✅ `ClientService` for CRUD operations
- ✅ Endpoints for creating/updating/listing clients
- ✅ Client synchronization with Profile Service

#### Auth User Management:
- ✅ `AuthUser` model
- ✅ Endpoints for creating auth users
- ✅ User validation endpoints
- ✅ Special handling for Eatta users (`createEattaAuthUser`)

### In Profile Service:

#### Profile Lookup:
- ✅ Lookup by country + phone number
- ✅ Lookup from M247 (money transfer service)
- ✅ Lookup from Fiorano Core Banking by:
  - National ID
  - Passport number
  - Account number
  - CIF (Customer ID)

#### Onboarding Workflow:
- ✅ `OnboardingStepName` enum (with multiple steps)
- ✅ `ProfileOnboardingService` for managing onboarding progress
- ✅ Step tracking and status management
- ✅ Device registration during onboarding
- ✅ OTP verification

#### PIN Management:
- ✅ PIN setup endpoint
- ✅ PIN validation (implicit with Cellulant)
- ✅ PIN storage in profile

#### Security Questions:
- ✅ `SecurityQuestion` entity
- ✅ Questions setup endpoint
- ✅ Questions validation endpoint

#### Device Management:
- ✅ `ProfileDevice` entity
- ✅ Device registration
- ✅ Device-specific operations (biometric challenges, etc.)
- ✅ DeviceId tracking

#### Fiorano Integration:
- ✅ Query by National ID
- ✅ Query by Passport
- ✅ Query by Account Number
- ✅ Query by CIF
- ✅ Token management
- ✅ Error handling

#### Cellulant Integration:
- ✅ Configuration and endpoint setup
- ✅ PIN validation service
- ✅ Authentication handling

---

## 8. CHANGES NEEDED FOR TRADE FINANCE USSD

### 8.1 PUBLIC AUTH SERVICE Changes

#### New Tenant Organization:
```
Name: "Trade Finance"
Code: "TRADE_FINANCE" (or similar)
Status: ACTIVE
Description: Trade Finance tenant for USSD-based onboarding
```

#### New Client:
```
Name: "Trade Finance USSD"
TenantOrganization: Trade Finance
ClientType: SHOULD BE EXTENDED (add new type if needed) OR use existing
Status: ACTIVE
Configuration: 
  - skipSecurityQuestions: true
  - skipCellulantValidation: true
  - onboardingType: "USSD"
```

#### Changes Required:
1. **Add new ClientType to enum** (if needed):
   - Option A: Add "USSD" type
   - Option B: Reuse existing and add config field

2. **Extend ClientDetails model** (if using config field approach):
   - Add optional configuration fields:
     - `skipSecurityQuestions: Boolean`
     - `skipCellulantPinValidation: Boolean`
     - `onboardingChannel: String` (USSD, MOBILE, WEB, etc.)

3. **Extend TenantOrganization or ClientDetails**:
   - Add metadata/configuration for onboarding preferences
   - OR create new `ClientOnboardingConfiguration` entity

### 8.2 PROFILE SERVICE Changes

#### Onboarding Flow Customization:
1. **Modify OnboardingStepName enum**:
   - Keep existing steps
   - These are still valid for other clients

2. **Extend ProfileOnboardingService**:
   - Check client configuration before adding `SecurityQuestionsSetUp` step
   - Skip step if `skipSecurityQuestions = true` for this client

3. **Modify OnboardingSteps/workflow logic**:
   - Create conditional logic based on client type/configuration
   - Skip `SecurityQuestionsSetUp` for Trade Finance USSD
   - Skip Cellulant PIN validation for Trade Finance USSD

4. **Create new endpoint or extend existing**:
   - Option A: Extend `ProfileCountryAndPhoneNoV1Controller.verifyProfilePhoneNo()`
   - Add clientId parameter to determine workflow
   - Option B: Create new Trade Finance specific endpoint

#### Fiorano Data Pull for Trade Finance USSD:

**New Flow - Trade Finance USSD Entry Point:**
```
User dials USSD code (Trade Finance)
    ↓
Controller receives: countryCode + phoneNumber + [clientId: "trade-finance-ussd"]
    ↓
Try to find profile in Profile Service:
    - Query by country + phone
    
IF Profile Found:
    └─→ Continue with existing profile + Trade Finance USSD workflow
    
IF Profile NOT Found:
    ├─→ Attempt Fiorano Core Banking Lookup
    │   Using: National ID OR Passport (must be provided by USSD app)
    │
    ├─→ IF Found in Fiorano:
    │   1. Extract customer details from Fiorano response
    │   2. Create new Profile with these details
    │   3. Store: customerName, email, idNumber, idType, phone, country, etc.
    │   4. Map to Trade Finance tenant
    │   5. Register device with ID: trade-finance-ussd-{countryCode}-{phoneNumber}
    │   6. Initialize onboarding:
    │      - Skip: IPRS Check, Document Upload, Liveness Check, KRA PIN, Astra Wallet, Account Opening
    │      - Include: OTP Verification (if needed), PIN Setup, Skip Security Questions
    │
    └─→ IF NOT Found in Fiorano:
        └─→ Return error: Customer not found in core banking
```

#### Key Changes Needed:

1. **Extend ProfileCountryAndPhoneNoService**:
   - Add method to handle Trade Finance USSD specific flow
   - New parameter: clientId or client configuration
   - Conditional Fiorano lookup with National ID/Passport

2. **New endpoint in ProfileCountryAndPhoneNoV1Controller**:
   ```
   POST /v1/profiles/country/{countryId}/phone-no/{phoneNo}/trade-finance-ussd
   Body: { nationalId/passport, clientId, etc. }
   Response: ProfileBlockInfoDTO with onboarding status
   ```

3. **Extend Fiorano integration**:
   - Method already exists: `getCustomerCoreBankingAccountsByIdNumberOrPassport()`
   - No changes needed to Fiorano service itself
   - USSD flow just needs to call this method

4. **Create Trade Finance specific onboarding step sequencer**:
   - File: `TradeFinanceOnboardingStepsServiceImpl`
   - OR: Extend `ProfileOnboardingStepsService` with conditional logic
   - Steps for Trade Finance USSD:
     1. OTP Verification (if needed)
     2. PIN Setup
     3. Confirm PIN
     4. Skip Security Questions
     5. Skip Cellulant Validation
     6. Complete onboarding

5. **Extend ProfileOnboardingService**:
   - Add client configuration awareness
   - Conditionally skip steps based on clientId/client config

#### Device ID Generation:

**New logic needed:**
```java
// In ProfileCountryAndPhoneNoService or device registration
if (client.getClientId().equals("trade-finance-ussd")) {
    deviceId = "trade-finance-ussd-" + countryCode + "-" + phoneNumber;
} else {
    // Existing logic
    deviceId = generateDefaultDeviceId();
}
```

#### OTP Verification:

**Check if needed for Trade Finance USSD:**
- Likely needed for phone verification before PIN setup
- May use different OTP provider
- Endpoints likely exist, just need to be called in correct order

---

## 9. DETAILED WORKFLOW FOR TRADE FINANCE USSD

### Complete Journey:

```
1. DIAL USSD CODE (e.g., *123#)
   Input: countryCode, phoneNumber (from USSD network)
   
2. PROFILE LOOKUP/CREATION
   POST /v1/profiles/country/{countryCode}/phone-no/{phoneNumber}/trade-finance-ussd
   Body: { clientId: "trade-finance-ussd" }
   
   Service Flow:
   a. Check if profile exists (countryCode + phone)
   b. If NOT found:
      - Prompt user for National ID or Passport
      - Query Fiorano with provided ID/Passport
      - If found in Fiorano: Extract customer details
      - If not found: Reject and exit
   c. Create/get Profile linked to Trade Finance tenant
   d. Register device with pattern: trade-finance-ussd-{countryCode}-{phone}
   e. Return onboarding status
   
3. OTP VERIFICATION
   POST /v1/profiles/{profileId}/otp/verify
   - Send OTP to phone number
   - User enters OTP
   - Verify and proceed
   
4. PIN SETUP
   POST /v1/profiles/{profileId}/pin/setup
   - Prompt user to set new PIN
   - Validate PIN strength
   - Store PIN in profile (hashed)
   
5. PIN CONFIRMATION
   POST /v1/profiles/{profileId}/pin/confirm
   - User re-enters PIN
   - Verify matches PIN setup
   
6. ONBOARDING COMPLETE
   ✅ Profile created with Fiorano data
   ✅ PIN set
   ✅ NO Security Questions (Skipped)
   ✅ NO Cellulant Validation (Skipped)
   ✅ Device registered
   ✅ Onboarding marked as COMPLETED
   
7. USER CAN NOW:
   - Access services via USSD
   - Use PIN for authentication
   - No need for security questions
```

---

## 10. CHANGES SUMMARY - BY SERVICE

### PUBLIC AUTH SERVICE:

**Files to Modify:**
1. `enums/ClientType.java` - Add new type OR create config model
2. `model/ClientDetails.java` - Add configuration fields (optional)
3. `controller/v1/TenantOrganizationV1Controller.java` - May need endpoint for creating new tenant
4. `controller/ClientController.java` - May need endpoint for creating Trade Finance client
5. `service/client/ClientService.java` - May need new methods for client configuration
6. `service/tenantorganization/TenantOrganizationService.java` - May need new methods

**Files to Create:**
1. `model/ClientOnboardingConfiguration.java` - (Optional, if config approach)
2. `dto/TradeFinanceClientConfigDTO.java` - DTO for Trade Finance config

**Database Changes:**
1. Migrate to add configuration fields to `client_details` table
2. OR create new `client_onboarding_config` table

### PROFILE SERVICE:

**Files to Modify:**
1. `controller/v1/ProfileCountryAndPhoneNoV1Controller.java` - Add Trade Finance specific endpoint
2. `service/profile/ProfileCountryAndPhoneNoService.java` - Add Trade Finance logic
3. `service/profile/ProfileCountryAndPhoneNoServiceImpl.java` - Implement Trade Finance flow
4. `service/profileonboarding/ProfileOnboardingService.java` - Add conditional step logic
5. `service/profileonboarding/ProfileOnboardingServiceImpl.java` - Implement conditional steps
6. `service/profileonboarding/ProfileOnboardingStepsService.java` - Add configuration awareness
7. `service/fiorano/FioranoDataQueryService.java` - Already has methods, no changes needed
8. `service/fiorano/FioranoDataQueryServiceImpl.java` - Already has methods, no changes needed

**Files to Create:**
1. `service/profileonboarding/TradeFinanceOnboardingStepsService.java` - Trade Finance specific steps
2. `service/profileonboarding/TradeFinanceOnboardingServiceImpl.java` - Implementation
3. `controller/v1/TradeFinanceProfileV1Controller.java` - (Optional, separate controller)
4. `dto/request/TradeFinanceOnboardingRequestDTO.java` - Request DTO

**Database Changes:**
1. No new table required (use existing structure)
2. Onboarding steps already configurable

---

## 11. DATA FLOW DIAGRAMS

### Current Architecture (for comparison):
```
User → USSD/Mobile/Web → ProfileCountryAndPhoneNoService
                           ↓
                    Try Profile lookup (countryCode + phone)
                           ↓
                    ┌─ Found → Use existing
                    │
                    └─ Not found → M247/Fiorano lookup
                           ↓
                           ↓ Account found
                           ↓
                    Create profile with standard steps
                    (including SecurityQuestions & Cellulant)
```

### New Architecture (Trade Finance USSD):
```
User dials USSD → USSD App → /v1/profiles/.../trade-finance-ussd
                               ↓
                        Check if profile exists
                               ↓
                        ┌─ Found → Get onboarding status
                        │
                        └─ Not found
                             ↓
                        User provides: National ID or Passport
                             ↓
                        Query Fiorano Core Banking
                             ↓
                        ┌─ Found in Fiorano
                        │    ↓
                        │ Extract: Name, Email, ID, Phone, etc.
                        │    ↓
                        │ Create Profile (mapped to Trade Finance tenant)
                        │    ↓
                        │ Register Device: trade-finance-ussd-{CC}-{PHONE}
                        │    ↓
                        │ Initialize Onboarding (Trade Finance specific):
                        │    - OTP Verification
                        │    - PIN Setup
                        │    - PIN Confirmation
                        │    - SKIP Security Questions ✖️
                        │    - SKIP Cellulant Validation ✖️
                        │    - Mark COMPLETED
                        │
                        └─ Not found in Fiorano
                             ↓
                        Return: Customer Not Found
```

---

## 12. BREAKING CHANGES RISK ASSESSMENT

### ✅ LOW RISK CHANGES (No impact on existing clients):
1. Adding new ClientType enum value
2. Creating new service implementations (Trade Finance specific)
3. Creating new controller endpoints (doesn't affect existing ones)
4. Adding optional configuration fields to ClientDetails

### ⚠️ MEDIUM RISK CHANGES (May need refactoring):
1. Extending ProfileOnboardingStepsService with conditional logic
   - **Mitigation**: Check clientId before skipping steps
   - **Testing**: Ensure existing clients still work (especially Eatta)
   
2. Extending ProfileCountryAndPhoneNoServiceImpl
   - **Mitigation**: Check clientId in method, fallback to existing logic
   - **Testing**: Integration tests with existing clients

### 🔴 HIGH RISK CHANGES (Requires careful handling):
1. Modifying OnboardingStepName enum
   - **Mitigation**: Don't remove existing steps, just conditionally skip
   - **Do NOT remove**: SecurityQuestionsSetUp
   
2. Modifying ProfileOnboardingService orchestration
   - **Mitigation**: Use configuration/strategy pattern to select correct flow
   - **Testing**: Comprehensive regression tests with all clients

---

## 13. CONFIGURATION APPROACH OPTIONS

### Option A: Enum-Based (Minimal DB changes)
```java
enum ClientType {
    Client, Partner, USSD, ...  // Add USSD type
}

// Check in service:
if (client.getClientType() == ClientType.USSD) {
    skipSecurityQuestions = true;
    skipCellulantValidation = true;
}
```
**Pros**: Simple, no new tables
**Cons**: Limited to predefined types

### Option B: Configuration Entity (Flexible)
```java
@Entity
class ClientOnboardingConfiguration {
    UUID clientId;
    boolean skipSecurityQuestions = false;
    boolean skipCellulantValidation = false;
    String deviceIdPattern;
    List<String> enabledOnboardingSteps;
    // ...
}
```
**Pros**: Highly flexible, reusable
**Cons**: New table, slightly more complex

### Option C: Hybrid (Recommended)
```java
// Add fields to ClientDetails:
class ClientDetails {
    // ... existing fields
    @Column(name = "onboarding_channel")
    String onboardingChannel;  // MOBILE, WEB, USSD, etc.
    
    @Column(name = "skip_security_questions")
    boolean skipSecurityQuestions = false;
    
    @Column(name = "skip_cellulant_validation")  
    boolean skipCellulantValidation = false;
}
```
**Pros**: Minimal schema change, configuration co-located with client
**Cons**: More fields on existing table

### Recommendation: **Option C (Hybrid)** - Simplest and most practical

---

## 14. IMPLEMENTATION ROADMAP (Not for execution yet)

### Phase 1: Setup
- [ ] Add Trade Finance tenant to Public Auth Service
- [ ] Add Trade Finance USSD client to Public Auth Service
- [ ] Add configuration fields to ClientDetails table

### Phase 2: Core Integration
- [ ] Create TradeFinanceOnboardingService
- [ ] Extend ProfileCountryAndPhoneNoService with Trade Finance logic
- [ ] Create new endpoint: `/v1/profiles/country/{cc}/phone-no/{pn}/trade-finance-ussd`

### Phase 3: Workflow Customization
- [ ] Implement conditional onboarding steps
- [ ] Skip SecurityQuestionsSetUp for Trade Finance USSD
- [ ] Skip Cellulant validation for Trade Finance USSD
- [ ] Implement Trade Finance device ID format

### Phase 4: Fiorano Integration
- [ ] Extend Trade Finance endpoint to accept National ID/Passport
- [ ] Call existing FioranoDataQueryService methods
- [ ] Map Fiorano response to Profile

### Phase 5: Testing & Validation
- [ ] Unit tests for Trade Finance service
- [ ] Integration tests with Fiorano
- [ ] Regression tests for existing clients (Eatta, etc.)
- [ ] E2E flow testing

---

## 15. KEY CONCERNS & CONSIDERATIONS

### 1. Profile Lookup:
- ❓ How does USSD app provide National ID/Passport initially?
- → Likely: Prompt user during initial USSD interaction
- → Or: USSD system provides from SIM/KYC data

### 2. OTP Verification:
- ❓ Is OTP needed before PIN setup for Trade Finance USSD?
- → Likely: YES (for phone number verification)
- → Existing endpoints should work

### 3. PIN Setup Without Cellulant:
- ❓ Where is PIN stored if not in Cellulant?
- → In Profile table (already has PIN field)
- → Cellulant validation is skipped, PIN is hashed and stored locally

### 4. Device ID Uniqueness:
- ❓ Is trade-finance-ussd-{CC}-{phone} unique?
- → YES: Combination of country + phone + client is unique per user

### 5. Tenant Isolation:
- ❓ Does Trade Finance data need isolated from other tenants?
- → YES: Use tenantId in all queries
- → Existing structure supports this

### 6. Backward Compatibility:
- ❓ Will changes break existing clients?
- → NO: If implemented with conditional logic
- → Existing clients should not have skipSecurityQuestions/skipCellulantValidation set (defaults to false)

---

## 16. TESTING STRATEGY (High Level)

### Unit Tests:
- TradeFinanceOnboardingService.selectOnboardingSteps()
- FioranoDataQueryService (already exists, works)
- DeviceId generation logic

### Integration Tests:
- Profile creation from Fiorano data
- Onboarding step flow with Trade Finance config
- Cellulant validation skip
- Security questions skip

### Regression Tests:
- Eatta USSD flow (if exists)
- Standard mobile flow
- All existing clients should work unchanged

### E2E Tests:
- Full Trade Finance USSD journey (country + phone → find in Fiorano → PIN setup → complete)
- Existing client flow (should be unchanged)

---

## 17. ENDPOINT SUMMARY

### Existing Endpoints That Will Be Used:
```
Profile Service:
  POST   /v1/profiles/country/{countryId}/phone-no/{phoneNo}
  GET    /v1/profiles/{profileId}/on-boardings/client/{clientId}
  POST   /v1/profiles/{profileId}/otp/send
  POST   /v1/profiles/{profileId}/otp/verify
  POST   /v1/profiles/{profileId}/pin/setup
  POST   /v1/profiles/{profileId}/pin/confirm
  
Public Auth Service:
  GET    /v1/tenant-organizations
  GET    /v1/clients
```

### New Endpoints Needed:
```
Profile Service:
  POST   /v1/profiles/country/{countryId}/phone-no/{phoneNo}/trade-finance-ussd
         Body: { nationalId or passport, clientId }
         Response: ProfileBlockInfoDTO (onboarding status)
  
  (Optional - might reuse existing with clientId parameter)
```

---

## CONCLUSION

This analysis shows that:

1. **The infrastructure already exists** for most of what's needed
2. **Fiorano integration is already built** - just needs to be called during Trade Finance USSD flow
3. **Main changes are conditional/configurational** - skip certain onboarding steps for this client
4. **Risk is LOW** if done with proper isolation (checking clientId/config before applying logic)
5. **Backward compatibility is maintained** - existing clients unaffected

**Next Step**: Implement the changes without breaking existing functionality.
