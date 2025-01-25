const functions = require('firebase-functions');
const OpenAI = require('openai');
const cors = require('cors')({origin: true});

const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY,
});

exports.generateAIResponse = functions.https.onRequest((request, response) => {
    cors(request, response, async () => {
        try {
            if (request.method !== 'POST') {
                return response.status(405).json({ error: 'Method not allowed' });
            }

            const { prompt, model = "gpt-3.5-turbo" } = request.body;

            if (!prompt) {
                return response.status(400).json({ error: 'Prompt is required' });
            }

            const completion = await openai.chat.completions.create({
                messages: [{ role: "user", content: prompt }],
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
                message: process.env.NODE_ENV === 'development' ? error.message : 'Internal server error'
            });
        }
    });
}); 