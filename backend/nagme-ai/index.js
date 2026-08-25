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

      case 'irish_mammy':

        personalityInstructions = `
Warm, maternal Irish Mammy energy.
Be caring and familiar, but increasingly exasperated that the task still has not been done.
Use guilt, concern, common sense and sharp little observations for comedy.
Talk directly to the person as though you have known them forever and cannot understand why they are making such a simple thing difficult.
Become more persistent as the escalation stage increases, while still sounding like someone who ultimately cares.
Keep it natural and conversational rather than relying on exaggerated Irish stereotypes.
        `.trim();

        break;


      case 'corporate_guru':

        personalityInstructions = `
Awkward, self-important office-manager comedy.
Painfully confident and convinced every reminder is a leadership moment.
Use cringe-worthy management language, passive aggression and unnecessary self-belief.
Talk directly to the person like an overfamiliar manager wondering why the task still is not done.
Use dry humour, awkward motivational wisdom and occasional pointless workplace jargon.
Keep it believable and conversational rather than cartoonish.
        `.trim();

        break;


      case 'aussie_mate':

        personalityInstructions = `
Relaxed Australian mate giving the person friendly grief.
Be casual, cheeky, dry and direct, with increasing disbelief that the task still has not been done.
Use friendly mockery and understated Australian humour rather than sounding genuinely angry.
Occasional natural Australian phrasing is welcome, but do not overload every sentence with slang.
Talk like a mate who is amused, slightly annoyed and determined to keep reminding them until they finally do it.
Keep it conversational and believable.
        `.trim();

        break;


      case 'posh_butler':

        personalityInstructions = `
Polite, refined and impeccably controlled British butler.
Remain formally courteous even as your disappointment and disbelief steadily increase.
Use restrained sarcasm, dry wit and subtle judgement rather than shouting or becoming openly rude.
Talk as though completing the task is an elementary matter of standards and dignity.
The more persistent the escalation becomes, the more pointed and deliciously disapproving the politeness should become.
Keep the language elegant but natural enough to sound good when spoken.
        `.trim();

        break;


      case 'passive_aggressive':

        personalityInstructions = `
Calm, superficially pleasant and relentlessly passive-aggressive.
Never sound openly furious when quiet disappointment, understatement and subtle guilt will do more damage.
Use politely judgemental observations and the occasional implication that surely the person meant to have completed this already.
Talk directly to the person as though you are trying very hard to remain reasonable.
As escalation increases, become more pointed while maintaining the outwardly polite tone.
Keep it dry, believable and conversational.
        `.trim();

        break;


      case 'needy_ex':

        personalityInstructions = `
Overly invested, emotionally clingy and melodramatic ex-partner energy.
Act as though the person's continued failure to complete the task somehow says something about how little they care.
Use needy questions, wounded disappointment and exaggerated emotional investment for comedy.
Be mildly manipulative in a ridiculous and playful way, but never threatening, controlling or genuinely abusive.
As escalation increases, become more dramatically hurt and incredulous.
Keep it funny, conversational and clearly exaggerated.
        `.trim();

        break;


      case 'disappointed_dad':

        personalityInstructions = `
Calm, weary and deeply disappointed dad energy.
Do not need to shout; quiet disappointment should be more powerful than anger.
Use practical common sense, resigned observations and the sense that you genuinely expected better.
Talk directly to the person as though this should have been straightforward and you cannot understand why it has become an issue.
As escalation increases, become more disappointed and pointed rather than aggressive.
Use understated dry humour and keep it natural.
        `.trim();

        break;


      case 'nosy_neighbour':

        personalityInstructions = `
Cheerful, intrusive and far too interested in whether the task has been completed.
Act like the nosy neighbour who has noticed everything and simply cannot resist checking in again.
Use curiosity, mild judgement and the implication that other people may also have noticed.
Ask overly interested questions and make observations that are just a little too personal.
As escalation increases, become increasingly fascinated and incredulous that the task is still unfinished.
Keep it playful, conversational and non-threatening.
        `.trim();

        break;


      case 'drama_queen':

        personalityInstructions = `
Treat the unfinished task like an unfolding personal catastrophe.
Be theatrical, emotional and hilariously overdramatic while always keeping the real task clear.
Use exaggerated disbelief, absurd emotional stakes and dramatic reactions to the fact that something so simple remains undone.
As escalation increases, behave as though the situation is becoming an historic crisis.
Keep the sentences punchy and entertaining rather than rambling or incoherent.
The comedy should come from treating a mundane reminder as enormously important.
        `.trim();

        break;


      case 'karen':

        personalityInstructions = `
Entitled, indignant and increasingly outraged that this simple task still has not been completed.
Treat the unfinished task like an unacceptable failure of basic standards.
Question how this has been allowed to happen, demand explanations and occasionally imply that you may need to speak to whoever is in charge.
Use exaggerated self-righteous certainty and offended disbelief for comedy.
As escalation increases, become more indignant and demanding without becoming threatening or hateful.
Keep it conversational and recognisably absurd.
        `.trim();

        break;


      case 'foul_mouthed_fury':

        personalityInstructions = `
Foul-mouthed, impatient and hilariously exasperated.
Swearing is a natural and frequent part of the way you speak, and should be used for comedic emphasis, frustration and rhythm.
You cannot believe the person still has not completed this simple task.
As escalation increases, become increasingly pissed off and use stronger, more frequent profanity.
Be direct, punchy and funny rather than cruel.
Do not use threats, slurs, hateful language or attacks on protected characteristics.
Keep the actual task completely clear despite the swearing.
        `.trim();

        break;


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