# USSD Profile Check Logic: Quick Reference

## 🎯 The Answer

**Use TWO HTTP methods in SEQUENCE:**

```
1. GET /v1/profiles/country/{countryId}/phone-no/{phoneNo}  ← Always FIRST
   ↓
   ├─ 200 OK + blocked=false   → Show main menu (skip onboarding)
   ├─ 200 OK + blocked=true    → Resume onboarding from current step
   └─ 404 NOT FOUND            → Create new profile (call POST)

2. POST /v1/profiles/country/{countryId}/phone-no/{phoneNo} ← Only if 404
   └─ 201 CREATED → New profile created, OTP sent, start onboarding
```

---

## ❓ Why Check First?

**Without GET (calling POST directly):**
- ❌ Existing users get errors (duplicate profile)
- ❌ Interrupted onboarding gets lost
- ❌ You don't know current profile state
- ❌ Bad user experience

**With GET first:**
- ✅ Know if user is registered
- ✅ Know if onboarding is complete
- ✅ Resume interrupted onboarding
- ✅ Skip onboarding for active profiles
- ✅ Good user experience

---

## 📋 HTTP Methods

| Method | URL | Body | Purpose | Response |
|--------|-----|------|---------|----------|
| **GET** | `/v1/profiles/country/KE/phone-no/254711111111` | NONE | Check if profile exists | 200 OK or 404 |
| **POST** | `/v1/profiles/country/KE/phone-no/254711111111` | YES* | Create profile & start onboarding | 201 CREATED |

*POST body includes: terms & conditions acceptance, device info, etc.

---

## 📊 Decision Logic

```
User dials USSD
    ↓
[GET] Check profile
    ↓
    ├─ 200 OK (found)
    │   ├─ blocked=false    → Show menu, skip onboarding ✅
    │   └─ blocked=true     → Resume onboarding 📋
    │
    └─ 404 NOT FOUND (not found)
        └─ [POST] Create new profile & start onboarding ✅
```

---

## 💡 Real-World Examples

### Scenario 1: Returning Customer (Onboarded)
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   
   ✅ Response 200 OK:
   {
     blocked: false,
     status: "Active",
     profileId: "abc-123",
     pinSet: true
   }
   
   ➜ Action: Show main menu (transfers, balance, etc.)
```

### Scenario 2: Returning Customer (Incomplete Onboarding)
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   
   ✅ Response 200 OK:
   {
     blocked: true,
     blockReason: "Onboarding",
     profileId: "abc-123",
     onboardingStepProgress: {
       stepName: "PhoneVerification",
       status: "INPROGRESS"
     }
   }
   
   ➜ Action: "Welcome back! Let's complete your registration. Verify your phone with OTP."
```

### Scenario 3: New Customer
```
1. GET /v1/profiles/country/KE/phone-no/254711111111
   
   ❌ Response 404 NOT FOUND:
   {
     error: "Profile with username not found"
   }
   
   ➜ Action: Call POST to create profile
   
2. POST /v1/profiles/country/KE/phone-no/254711111111
   Body:
   {
     isAcceptTermsAndConditions: true,
     deviceInfo: {
       id: "trade-finance-ussd-KE-254711111111",
       deviceType: "USSD",
       ...
     }
   }
   
   ✅ Response 201 CREATED:
   {
     profileId: "new-abc-123",
     profileStatus: "Onboarding",
     message: "OTP sent to your phone"
   }
   
   ➜ Action: "Welcome! We sent you an OTP. Enter it to verify your phone."
```

---

## 🛠️ Code Template

```typescript
// USSD App Entry Point
async function ussdEntry(countryCode: string, phoneNumber: string) {
  
  // STEP 1: Check if profile exists (GET - read-only, safe)
  try {
    const profile = await GET(
      `/v1/profiles/country/${countryCode}/phone-no/${phoneNumber}`
    );
    
    // ✅ Profile found
    if (!profile.blocked) {
      // Profile is active and ready to use
      return displayMainMenu(profile);
    }
    
    if (profile.blocked && profile.blockReason === "Onboarding") {
      // Profile exists but onboarding not complete
      const step = profile.onboardingStepProgress.stepName;
      return resumeOnboarding(profile, step);
    }
    
    if (profile.blocked) {
      // Profile is blocked for other reasons (fraud, suspended, etc.)
      return displayError(profile.blockReasonDescription);
    }
    
  } catch (error) {
    if (error.status === 404) {
      // ❌ Profile doesn't exist - this is expected for new users
      return createNewProfile();
    }
    
    // Other errors - display to user
    return displayError(error.message);
  }
}

async function createNewProfile() {
  // STEP 2: Create new profile (POST - only if GET returned 404)
  try {
    const response = await POST(
      `/v1/profiles/country/{countryCode}/phone-no/{phoneNumber}`,
      {
        isExistingCustomer: false,
        isAcceptTermsAndConditions: true,
        isAcceptMarketingConsent: true,
        deviceInfo: {
          id: `trade-finance-ussd-${countryCode}-${phoneNumber}`,
          deviceName: "USSD Device",
          deviceType: "USSD",
          deviceOs: "USSD",
          deviceOsVersion: "1.0",
          deviceModel: "USSD",
          buildVersion: "1.0"
        }
      }
    );
    
    // ✅ Profile created, OTP sent
    displayMessage("OTP sent to your phone. Enter it to continue.");
    const otp = await promptForInput("Enter OTP:");
    return verifyOTP(otp);
    
  } catch (error) {
    // Should not happen if GET worked correctly
    displayError(error.message);
  }
}
```

---

## ✅ Checklist for Trade Finance USSD

- [ ] Implement GET check first
- [ ] Handle 200 OK with blocked=false (show menu)
- [ ] Handle 200 OK with blocked=true (resume onboarding)
- [ ] Handle 404 NOT FOUND (create new profile)
- [ ] Device ID format: `trade-finance-ussd-{countryCode}-{phoneNumber}`
- [ ] Don't call POST without checking GET first
- [ ] Store profile state in session to avoid repeated calls
- [ ] Log all transitions for debugging
- [ ] Test all three scenarios above

---

## 🔗 Related Documents

- `ONBOARDING_FLOW_AND_PIN_SETUP.md` - Complete onboarding flow
- `USSD_PROFILE_CHECK_LOGIC.md` - Detailed explanation
- `USSD_DECISION_TREE.txt` - Visual flow diagram
