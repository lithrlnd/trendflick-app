const express = require('express');
const router = express.Router();
const aiController = require('../controllers/ai.controller');

// Route for generating AI responses
router.post('/generate', aiController.generateResponse);

module.exports = router; 