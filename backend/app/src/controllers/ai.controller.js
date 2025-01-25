const openai = require('../config/openai');

const generateResponse = async (req, res) => {
    try {
        const { prompt, model = "gpt-3.5-turbo" } = req.body;

        if (!prompt) {
            return res.status(400).json({ error: 'Prompt is required' });
        }

        const completion = await openai.chat.completions.create({
            messages: [{ role: "user", content: prompt }],
            model: model,
        });

        res.json({
            success: true,
            data: completion.choices[0].message.content
        });

    } catch (error) {
        console.error('OpenAI API Error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to generate AI response',
            message: process.env.NODE_ENV === 'development' ? error.message : 'Internal server error'
        });
    }
};

module.exports = {
    generateResponse
}; 