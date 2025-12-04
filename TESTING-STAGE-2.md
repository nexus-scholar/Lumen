# Testing Stage 2: Research Questions Generation

## ✅ Build Fixed - Ready to Test!

The issue where Stage 2 remained disabled after PICO approval has been **FIXED**.

---

## How to Test

### 1. Run the Application
```bash
.\gradlew.bat run
```

### 2. Open Your Project
- Navigate to your existing project: `project_1764766073_8119`
- Or create a new project

### 3. Test PICO Approval (Stage 1)
- Your PICO is already approved (see `ProblemFraming.json`)
- If you need to re-test:
  1. Click "View" on Stage 1 card
  2. Click "Approve & Continue"
  3. **Watch Stage 2 become enabled!** ✨

### 4. Test Research Questions Generation (Stage 2)

**Option A: With Working LLM API**
1. Fix your OpenRouter API key (see instructions below)
2. Click "Run" on Stage 2 card
3. Wait for questions to generate (~10-30 seconds)
4. Click "View & Approve" to review
5. Edit if needed, then click "Approve & Continue"

**Option B: Manual Entry (No API needed)**
1. Click "Run" on Stage 2 card
2. It will fail but create empty template
3. Click "View & Approve" button
4. Manually enter your research questions:
   - **Primary:** "How does domain shift affect the diagnostic accuracy of deep learning models for plant disease detection in field-grown crops across different geographic regions?"
   - **Secondary 1:** "What are the key factors contributing to domain shift in plant disease detection models?"
   - **Secondary 2:** "How do different deep learning architectures perform under domain shift conditions?"
   - **Secondary 3:** "What domain adaptation techniques improve model performance across geographic regions?"
5. Click "Approve & Continue"
6. **Stage 3 (Concept Expansion) should now be enabled!** ✨

---

## Fixing Your OpenRouter API Key

Your current error shows "User not found" (401), which means invalid API key.

### Quick Fix:
1. Go to https://openrouter.ai/keys
2. Sign in or create account
3. Create a new API key
4. Copy the key (starts with `sk-or-v1-...`)
5. Set environment variable:
   ```powershell
   $env:OPENROUTER_API_KEY = "sk-or-v1-YOUR_NEW_KEY_HERE"
   ```
6. Restart the app
7. Try Stage 2 again - it should work!

---

## What Was Fixed

### The Problem:
- PICO was approved (`"approved": true` in JSON)
- But Stage 2 remained disabled (greyed out Run button)
- This was because the **project status** wasn't being updated

### The Solution:
1. **`approvePico()` now updates project status**
   - Sets status to `RESEARCH_QUESTIONS`
   - Saves updated project to `Project.json`

2. **UI automatically refreshes**
   - Added `reloadTrigger` state
   - Project reloads when PICO is approved
   - Stage 2 immediately becomes enabled

3. **Callback chain works**
   - `PicoApprovalDialog` → `approvePico()` → `onProjectUpdate()` → reload project
   - UI reflects changes instantly

---

## Expected Behavior

### Before Approving PICO:
```
Stage 1: PICO Extraction    [Enabled ✓]  [Run]
Stage 2: Research Questions [Disabled 🔒] (greyed out)
Stage 3: Concept Expansion  [Disabled 🔒]
```

### After Approving PICO:
```
Stage 1: PICO Extraction    [Completed ✓] 
Stage 2: Research Questions [Enabled ✓]   [Run] ← NOW ENABLED!
Stage 3: Concept Expansion  [Disabled 🔒]
```

### After Approving Questions:
```
Stage 1: PICO Extraction    [Completed ✓]
Stage 2: Research Questions [Completed ✓]
Stage 3: Concept Expansion  [Enabled ✓]   [Run] ← Should work!
```

---

## Troubleshooting

### Stage 2 Still Disabled?
1. Check `data/project_XXX/artifacts/Project.json`
2. Look for `"status": "RESEARCH_QUESTIONS"`
3. If it still says `"PICO_EXTRACTION"`, re-approve PICO

### Can't Click "View & Approve"?
1. Make sure you clicked "Run" first
2. Check console for error messages
3. Even if LLM fails, the button should appear

### Questions Not Saving?
1. Check `data/project_XXX/artifacts/` folder
2. Look for `ResearchQuestions.json`
3. Check console for save errors

---

## Success Indicators

✅ **Stage 2 becomes enabled** immediately after PICO approval  
✅ **Run button clickable** on Stage 2 card  
✅ **Questions generate** (with API) or **empty form shows** (without API)  
✅ **View & Approve button** appears after running  
✅ **Can edit questions** in the dialog  
✅ **Can add/remove** secondary questions  
✅ **Questions save** to `ResearchQuestions.json`  
✅ **Stage 3 becomes enabled** after approving questions  

---

## Your PICO is Already Perfect!

Based on your `ProblemFraming.json`:

**Population:** Field-grown crops (wheat, tomato, soybean) with plant diseases  
**Intervention:** Deep learning-based disease detection trained on source domain  
**Comparison:** Traditional methods or target domain models  
**Outcome:** Diagnostic accuracy under domain shift conditions  

This is excellent! The research questions should focus on:
1. Main effect of domain shift on accuracy
2. Contributing factors to domain shift
3. Performance across different architectures
4. Domain adaptation techniques

---

## Next Steps After Testing

1. ✅ Test PICO approval → Stage 2 enables
2. ✅ Test research questions generation (manual or LLM)
3. ✅ Test question editing and approval
4. ✅ Verify Stage 3 becomes enabled
5. 📝 Report any issues you find

---

**The build is fixed and ready! Test it now!** 🚀

If you encounter any issues, let me know and I'll fix them immediately.

