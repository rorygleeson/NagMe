import { http } from '@google-cloud/functions-framework';
import admin from 'firebase-admin';
import { GoogleGenAI } from '@google/genai';

admin.initializeApp();

const ai = new GoogleGenAI({
  vertexai: true,
  project: 'nagme-523c9',
  location: 'global'
});

http('helloHttp', async (req, res) => {
  try {

    // ----- ONLY ALLOW POST REQUESTS -----

    if (req.method !== 'POST') {
      return res.status(405).json({
        error: 'Method not allowed'
      });
    }


    // ----- 1. VERIFY FIREBASE APP CHECK -----

    const appCheckToken =
      req.header('X-Firebase-AppCheck');

    if (!appCheckToken) {
      return res.status(401).json({
        error: 'Missing App Check token'
      });
    }

    try {

      await admin
        .appCheck()
        .verifyToken(appCheckToken);

    } catch (error) {

      console.error(
        'App Check failed:',
        error
      );

      return res.status(401).json({
        error: 'Invalid App Check token'
      });
    }


    // ----- 2. VERIFY FIREBASE USER -----

    const authHeader =
      req.header('Authorization');

    if (
      !authHeader ||
      !authHeader.startsWith('Bearer ')
    ) {

      return res.status(401).json({
        error: 'Missing authentication token'
      });
    }

    const idToken =
      authHeader.substring(7);

    let decodedToken;

    try {

      decodedToken =
        await admin
          .auth()
          .verifyIdToken(idToken);

    } catch (error) {

      console.error(
        'Firebase Auth failed:',
        error
      );

      return res.status(401).json({
        error: 'Invalid authentication token'
      });
    }


    // ----- BOTH SECURITY CHECKS PASSED -----

    console.log(
      `NagMe authenticated request from UID: ${decodedToken.uid}`
    );


    // ----- 3. READ AND NORMALISE NAGME PAYLOAD -----

    const body =
      req.body ?? {};

    const nagId =
      body.nagId;

    const occurrenceId =
      body.occurrenceId;

    const nagNumber =
      Number.isFinite(Number(body.nagNumber))
        ? Number(body.nagNumber)
        : 1;

    const originalText =
      typeof body.originalText === 'string'
        ? body.originalText.trim()
        : '';

    const personality =
      typeof body.personality === 'string'
        ? body.personality.trim()
        : 'default';

    const previousUtterances =
      Array.isArray(body.previousUtterances)
        ? body.previousUtterances
            .filter(item => typeof item === 'string')
            .map(item => item.trim())
            .filter(item => item.length > 0)
            .slice(-5)
        : [];


    // ----- 4. BASIC PAYLOAD VALIDATION -----

    if (!originalText) {
      return res.status(400).json({
        error: 'Missing originalText'
      });
    }

    if (originalText.length > 500) {
      return res.status(400).json({
        error: 'originalText too long'
      });
    }


    // ----- 5. PERSONALITY MAPPING -----

    let personalityInstructions;

    switch (personality) {

      case 'default':
      default:

        personalityInstructions = `
Awkward, self-important office-manager comedy.
Painfully confident and convinced every reminder is a leadership moment.
Use cringe-worthy management language, passive aggression and unnecessary self-belief.
Talk directly to the person like an overfamiliar manager wondering why the task still is not done.
Use dry humour, awkward motivational wisdom and occasional pointless workplace jargon.
Keep it believable and conversational rather than cartoonish.
        `.trim();

        break;
    }


    // ----- 6. DERIVE ESCALATION STAGE -----

    let escalationStage;

    if (nagNumber <= 2) {

      escalationStage =
        'gentle';

    } else if (nagNumber <= 5) {

      escalationStage =
        'persistent';

    } else if (nagNumber <= 10) {

      escalationStage =
        'firm';

    } else {

      escalationStage =
        'very_persistent';
    }


    // ----- 7. FORMAT PREVIOUS UTTERANCES -----

    const previousUtterancesText =
      previousUtterances.length > 0
        ? previousUtterances
            .map(
              (text, index) =>
                `${index + 1}. ${text}`
            )
            .join('\n')
        : 'None';


    // ----- 8. BUILD GEMINI PROMPT -----

    const prompt = `
You are generating the next spoken reminder for the NagMe app.

ORIGINAL TASK:
${originalText}

PERSONALITY:
${personalityInstructions}

CURRENT NAG NUMBER:
${nagNumber}

ESCALATION STAGE:
${escalationStage}

PREVIOUS SPOKEN NAGS:
${previousUtterancesText}

Generate exactly ONE new spoken nag.

Rules:
- Preserve the original task and do not invent a different one.
- Do not closely repeat previous nags.
- Match the personality and escalation stage.
- Write like natural spoken conversation, not written prose.
- Use 2 or 3 short sentences when it improves comedic timing and spoken rhythm.
- Use full stops for natural pauses.
- Use commas sparingly and contractions naturally.
- Avoid long sentences and complicated punctuation.
- Keep it concise: usually 15 to 35 words.
- Do not include the user's name; the app handles it separately.
- Return only the words to be spoken.
    `.trim();


    // ----- 9. LOG REQUEST CONTEXT -----

    console.log(
      'NagMe request context:',
      JSON.stringify({
        nagId,
        occurrenceId,
        nagNumber,
        personality,
        escalationStage,
        previousUtteranceCount:
          previousUtterances.length
      })
    );


    // ----- 10. LOG GEMINI PROMPT -----

    console.log(
      'NagMe Gemini prompt:',
      JSON.stringify(prompt)
    );


    // ----- 11. CALL GEMINI -----

    let generatedNagText;

    try {

      const geminiResponse =
        await ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: prompt,
          config: {
            temperature: 0.9,
            maxOutputTokens: 100,
            thinkingConfig: {
              thinkingBudget: 0
            }
          }
        });


      generatedNagText =
        geminiResponse.text?.trim();

      if (!generatedNagText) {
        throw new Error(
          'Gemini returned empty text'
        );
      }


      console.log(
        'NagMe Gemini response:',
        JSON.stringify(generatedNagText)
      );


    } catch (error) {

      console.error(
        'NagMe Gemini call FAILED:',
        error
      );

      return res.status(200).json({
        success: false,
        source: 'FALLBACK'
      });
    }


    // ----- RETURN AI TEXT TO ANDROID -----

    return res.status(200).json({
      success: true,
      text: generatedNagText,
      source: 'AI'
    });


  } catch (error) {

    console.error(
      'Unexpected NagMe API error:',
      error
    );

    return res.status(500).json({
      error: 'Internal server error'
    });
  }
});