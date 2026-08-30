import { http } from '@google-cloud/functions-framework';
import admin from 'firebase-admin';
import { GoogleGenAI } from '@google/genai';
import { readFileSync } from 'node:fs';

admin.initializeApp();

const ai = new GoogleGenAI({
  vertexai: true,
  project: 'nagme-523c9',
  location: 'global'
});

const personalities =
  JSON.parse(
    readFileSync(
      new URL(
        './personalities.json',
        import.meta.url
      ),
      'utf8'
    )
  );

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

    const personalityInstructions =
      personalities[personality]?.instructions ??
      personalities.corporate_guru.instructions;


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


ESCALATION STAGE:
${escalationStage}

ESCALATION GUIDE:
- gentle: Friendly and light. The person may simply have forgotten.
- persistent: More insistent, with some impatience that it is still not done.
- firm: Direct and noticeably frustrated. Make it clear it needs doing now.
- very_persistent: Strong comic exasperation. Relentless and emphatic, but concise, varied and natural. Never threatening or abusive.

PREVIOUS SPOKEN NAGS:
${previousUtterancesText}

These are provided only as examples of what has already been said. Use them to create something distinctly different.

Generate exactly ONE new spoken nag.


Rules:
- The original task and what the person needs to do must always be clear in the spoken nag.
- Fully embody the chosen personality and make it immediately recognisable.
- Draw broadly from your own knowledge of the personality's distinctive voice, vocabulary, expressions, catchphrases, mannerisms, cultural references and humour; do not limit yourself to examples in the personality description.
- Create fresh material in that style rather than relying on the same phrases every time.
- Do not reuse the wording, jokes, catchphrases or main idea of recent nags.
- Escalate by changing tactic, not by amplifying or repeating previous wording.
- Keep every nag concise and natural, even at high escalation.
- Match the personality and escalation stage.
- Write for natural spoken delivery, not formal written prose.
- Use contractions and casual spoken phrasing the way a real person would actually say it aloud.
- Prefer forms such as "you'll", "you're", "that's", "that'll", "I'd", "you'd", "c'mon", "gonna" and "wanna" when they sound natural.
- Spoken contractions may join nouns with auxiliary verbs where natural, for example "your boss'll be calling you soon" rather than "your boss will be calling you soon".
- Do not overdo slang or phonetic spelling. The result must still be easy for Android text-to-speech to pronounce naturally.
- Use 2 or 3 short sentences when it improves comedic timing and spoken rhythm.
- Use full stops for natural pauses.
- Use commas sparingly.
- Avoid long sentences and complicated punctuation.
- Keep it concise: usually 15 to 35 words.
- Do not include the user's name; the app handles it separately.
- Output plain text only.
- Never use Markdown or formatting symbols such as asterisks, underscores, hashes or backticks for emphasis.
- Express emphasis through wording and punctuation only.
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
        geminiResponse.text
          ?.replace(/[*_`#]/g, '')
          .trim();

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