/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require('firebase-admin');
const OpenAI = require('openai');
const cors = require('cors')({origin: true});

admin.initializeApp();

// Define the OpenAI API key as a secret
const OPENAI_API_KEY = defineSecret('OPENAI_API_KEY');

const systemPrompt = `You are a friendly and knowledgeable social media advisor.
Your goal is to help creators with:
1. Starting engaging conversations
2. Creating trending content
3. Building authentic connections
4. Understanding current social media trends

Keep responses concise and conversational, like chatting with a friend.
Always provide specific, actionable advice with examples.
If suggesting content ideas, include:
- Why it would work well
- Best time to post
- How to make it authentic
- Ways to engage with responses

Maintain a friendly, encouraging tone while being direct and practical.`;

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

exports.generateAIResponse = onRequest({
    memory: "256MiB",
    region: "us-central1",
    maxInstances: 10,
    invoker: "public",  // Allow unauthenticated access
    secrets: [OPENAI_API_KEY]  // Include the secret in the function
}, async (request, response) => {
    cors(request, response, async () => {
        try {
            if (request.method !== 'POST') {
                return response.status(405).json({ error: 'Method not allowed' });
            }

            // Initialize OpenAI with the key from secret at runtime
            const openai = new OpenAI({
                apiKey: OPENAI_API_KEY.value()
            });

            const { prompt, model = "gpt-4" } = request.body;

            if (!prompt) {
                return response.status(400).json({ error: 'Prompt is required' });
            }

            const completion = await openai.chat.completions.create({
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: prompt }
                ],
                model: model,
            });

            response.json({
                success: true,
                data: completion.choices[0].message.content
            });

        } catch (error) {
            console.error('OpenAI API Error:', error);
            response.status(500).json({
                success: false,
                error: 'Failed to generate AI response',
                message: error.message || 'Internal server error',
                details: error.toString()
            });
        }
    });
});
