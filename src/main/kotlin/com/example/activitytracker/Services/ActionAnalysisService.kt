package com.example.activitytracker.Services

import com.example.activitytracker.Action
import com.example.activitytracker.AppAction
import com.example.activitytracker.KeyboardAction
import com.example.activitytracker.MouseAction
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.RestClientException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlin.collections.emptyList

@Service
class ActionAnalysisService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Value("\${ollama.url:http://ollama:11434}")
    private lateinit var ollamaUrl: String

    @Value("\${ai.model.name:llama3.2:3b}")
    private lateinit var modelName: String

    @Value("\${ai.enabled:true}")
    private var aiEnabled: Boolean = true

    @Value("\${ai.fallback.no-model:true}")
    private var fallbackWhenNoModel: Boolean = true

    private val modelsReady = AtomicBoolean(false)
    private var modelPullInProgress = AtomicBoolean(false)
    private val analysisCache = ConcurrentHashMap<String, CachedAnalysis>()

    data class CachedAnalysis(
        val result: AnalysisResult,
        val timestamp: Long,
        val actionIdsHash: Int
    )

    data class AnalysisResult(
        val is_anomalous: Boolean,
        val confidence: Double,
        val fraud_probability: Double,
        val issues: List<String>,
        val analysis: String,
        val recommended_action: String,
        val processing_time_ms: Long = 0
    )

    data class OllamaGenerateRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean = false,
        val options: Map<String, Any> = mapOf(
            "temperature" to 0.3,
            "top_p" to 0.9,
            "max_tokens" to 500
        )
    )

    data class OllamaGenerateResponse(
        val response: String,
        val total_duration: Long? = null,
        val eval_count: Int? = null
    )

    @PostConstruct
    fun initialize() {
        if (!aiEnabled) {
            logger.info("AI analysis is disabled")
            return
        }

        scope.launch {
            delay(5000) // Give Ollama time to start
            checkAndPullModel()
        }
    }

    private suspend fun checkAndPullModel() {
        if (modelsReady.get() || modelPullInProgress.get()) return

        modelPullInProgress.set(true)

        try {
            logger.info("Checking if model '$modelName' is available...")

            if (isModelAvailable()) {
                logger.info("Model '$modelName' is already available")
                modelsReady.set(true)
                return
            }

            logger.info("Model '$modelName' not found. Pulling model (this may take several minutes on first run)...")
            pullModel()

            val maxAttempts = 300   // 300 * 2 = 10 minutes
            var attempts = 0
            var modelReady = false
            while (attempts < maxAttempts && !modelReady) {
                delay(2000)
                attempts++
                modelReady = isModelAvailable()
                if (modelReady) {
                    logger.info("Model '$modelName' is ready after ${attempts * 2} seconds!")
                    break
                }
                if (attempts % 15 == 0) { // Log every 30 seconds
                    logger.info("Still waiting for model '$modelName' to download... (${attempts * 2}s elapsed)")
                }
            }

            if (modelReady) {
                modelsReady.set(true)
            } else {
                logger.error("Model '$modelName' did not become ready after 10 minutes")
                if (fallbackWhenNoModel) {
                    logger.warn("Running in fallback mode - rule-based analysis only")
                }
            }

        } catch (e: Exception) {
            logger.error("Error during model initialization: ${e.message}", e)
        } finally {
            modelPullInProgress.set(false)
        }
    }

    private fun isModelAvailable(): Boolean {
        return try {
            val url = "$ollamaUrl/api/tags"
            val response = restTemplate.getForObject(url, Map::class.java)
            val models = response?.get("models") as? List<*> ?: emptyList<Any?>()
            models.any { model ->
                (model as? Map<*, *>)?.get("name") == modelName ||
                        (model as? Map<*, *>)?.get("name")?.toString()?.startsWith(modelName) == true
            }
        } catch (e: Exception) {
            logger.debug("Failed to check model availability: ${e.message}")
            false
        }
    }

    private fun pullModel() {
        try {
            val url = "$ollamaUrl/api/pull"
            val request = mapOf("name" to modelName)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(request, headers)

            // This is a long-running operation
            restTemplate.postForObject(url, entity, String::class.java)
        } catch (e: Exception) {
            logger.error("Failed to pull model: ${e.message}", e)
            throw e
        }
    }

    suspend fun analyzeActions(
        actions: List<Action>,
        userId: Long,
        forceFresh: Boolean = false
    ): AnalysisResult {
        val startTime = System.currentTimeMillis()

        // Check cache
        val actionHash = actions.take(100).hashCode() // Cache based on first 100 actions
        if (!forceFresh) {
            val cached = analysisCache["$userId-$actionHash"]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < 300000) { // 5 min cache
                logger.debug("Returning cached analysis for user $userId")
                return cached.result
            }
        }

        val result = if (!aiEnabled || !modelsReady.get()) {
            // Fallback to rule-based analysis
            logger.debug("Using fallback rule-based analysis for user $userId")
            ruleBasedAnalysis(actions, userId)
        } else {
            try {
                withTimeout(30000) { // 30 second timeout
                    llmBasedAnalysis(actions, userId)
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("LLM analysis timed out for user $userId, using fallback")
                ruleBasedAnalysis(actions, userId).copy(
                    analysis = "LLM analysis timed out, using rule-based fallback"
                )
            } catch (e: Exception) {
                logger.error("LLM analysis failed for user $userId: ${e.message}", e)
                ruleBasedAnalysis(actions, userId).copy(
                    analysis = "LLM analysis failed: ${e.message}, using fallback"
                )
            }
        }

        val finalResult = result.copy(processing_time_ms = System.currentTimeMillis() - startTime)

        // Cache result
        analysisCache["$userId-$actionHash"] = CachedAnalysis(
            result = finalResult,
            timestamp = System.currentTimeMillis(),
            actionIdsHash = actionHash
        )

        // Clean old cache entries
        if (analysisCache.size > 100) {
            val oldEntries = analysisCache.entries.filter {
                System.currentTimeMillis() - it.value.timestamp > 3600000 // 1 hour
            }
            oldEntries.forEach { analysisCache.remove(it.key) }
        }

        return finalResult
    }

    private suspend fun llmBasedAnalysis(actions: List<Action>, userId: Long): AnalysisResult {
        val prompt = buildAnalysisPrompt(actions, userId)

        val request = OllamaGenerateRequest(
            model = modelName,
            prompt = prompt
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
        val url = "$ollamaUrl/api/generate"

        try {
            val response = restTemplate.postForObject(url, entity, Map::class.java)
            val responseText = response?.get("response") as? String ?: ""

            return parseAnalysisResponse(responseText, userId)
        } catch (e: RestClientException) {
            logger.error("Ollama API call failed: ${e.message}", e)
            throw e
        }
    }

    private fun buildAnalysisPrompt(actions: List<Action>, userId: Long): String {
        // Limit actions to avoid token overflow (Llama 3.2 3B has 128k context, but keep reasonable)
        val relevantActions = actions.takeLast(500)

        val actionSummary = relevantActions.joinToString("\n") { action ->
            when (action) {
                is AppAction -> "${action.performedAt}: APP ${action.app_name}"
                is MouseAction -> {
                    if (action.is_click) {
                        "${action.performedAt}: MOUSE_CLICK at (${action.delta_x}, ${action.delta_y})"
                    } else {
                        "${action.performedAt}: MOUSE_MOVE to (${action.delta_x}, ${action.delta_y})"
                    }
                }
                is KeyboardAction -> "${action.performedAt}: KEYBOARD input"
                else -> "${action.performedAt}: UNKNOWN action"
            }
        }

        // Calculate basic stats for context
        val appCount = actions.filterIsInstance<AppAction>().size
        val mouseClicks = actions.filterIsInstance<MouseAction>().count { it.is_click }
        val mouseMoves = actions.filterIsInstance<MouseAction>().count { !it.is_click }
        val keyboardCount = actions.filterIsInstance<KeyboardAction>().size

        return """
            Analyze these user actions for potential fraud, anomalies, or suspicious patterns.
            
            User ID: $userId
            Time period: ${relevantActions.firstOrNull()?.performedAt} to ${relevantActions.lastOrNull()?.performedAt}
            
            Statistics:
            - Total actions: ${relevantActions.size}
            - App switches: $appCount
            - Mouse clicks: $mouseClicks
            - Mouse movements: $mouseMoves
            - Keyboard inputs: $keyboardCount
            - Click-to-move ratio: ${if (mouseMoves > 0) mouseClicks.toDouble() / mouseMoves else "N/A"}
            
            Action sequence:
            $actionSummary
            
            Return ONLY valid JSON with this exact structure (no markdown, no extra text):
            {
                "is_anomalous": boolean,
                "confidence": float between 0 and 1,
                "fraud_probability": float between 0 and 1,
                "issues": ["issue1", "issue2"],
                "analysis": "brief explanation of findings",
                "recommended_action": "none|warn|flag|block"
            }
            
            Look for:
            - Unusual patterns (too rapid actions, impossible sequences)
            - Bot-like behavior (perfect timing, repetitive patterns)
            - Potential fraud (unusual app access, strange navigation)
            - Working hour violations
            - Idle/login anomalies
        """.trimIndent()
    }

    private fun parseAnalysisResponse(responseText: String, userId: Long): AnalysisResult {
        return try {
            // Extract JSON from response (in case model added extra text)
            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}') + 1
            val jsonString = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                responseText.substring(jsonStart, jsonEnd)
            } else {
                responseText
            }

            objectMapper.readValue<AnalysisResult>(jsonString)
        } catch (e: Exception) {
            logger.error("Failed to parse LLM response: ${e.message}\nResponse: $responseText")
            // Return default analysis
            AnalysisResult(
                is_anomalous = false,
                confidence = 0.0,
                fraud_probability = 0.0,
                issues = listOf("Failed to parse AI response"),
                analysis = "AI analysis failed to parse: ${e.message}",
                recommended_action = "none"
            )
        }
    }

    private fun ruleBasedAnalysis(actions: List<Action>, userId: Long): AnalysisResult {
        val mouseActions = actions.filterIsInstance<MouseAction>()
        val keyboardActions = actions.filterIsInstance<KeyboardAction>()
        val appActions = actions.filterIsInstance<AppAction>()

        val issues = mutableListOf<String>()
        var fraudScore = 0.0

        // Heuristic 1: Too many clicks without movement
        val clicksWithNoMovement = mouseActions.count { it.is_click && it.delta_x.toInt() == 0 && it.delta_y.toInt() == 0 }
        if (mouseActions.isNotEmpty() && clicksWithNoMovement.toDouble() / mouseActions.size > 0.8) {
            issues.add("Suspicious: 80%+ clicks have no mouse movement")
            fraudScore += 0.3
        }

        // Heuristic 2: Unusual app switching frequency
        if (appActions.size > 100 && actions.size < 500) {
            issues.add("Unusual: High frequency of app switching (${appActions.size} switches)")
            fraudScore += 0.2
        }

        // Heuristic 3: Bot-like timing (actions too uniform)
        val timestamps = actions.map { it.performedAt.toEpochSecond(java.time.ZoneOffset.UTC) }
        if (timestamps.size > 10) {
            val differences = timestamps.zipWithNext { a, b -> b - a }.filter { it > 0 }
            if (differences.isNotEmpty()) {
                val stdDev = calculateStdDev(differences)
                val mean = differences.average()
                if (stdDev < mean * 0.1 && mean in 1.0..5.0) {
                    issues.add("Bot-like: Actions have very uniform timing (std dev: $stdDev)")
                    fraudScore += 0.4
                }
            }
        }

        // Heuristic 4: No keyboard activity but many clicks
        if (keyboardActions.isEmpty() && mouseActions.size > 50) {
            issues.add("Suspicious: No keyboard activity with $mouseActions mouse actions")
            fraudScore += 0.2
        }

        // Heuristic 5: Extreme click speed
        val clicks = mouseActions.filter { it.is_click }
        if (clicks.size > 50 && timestamps.size > 1) {
            val duration = timestamps.last() - timestamps.first()
            val clicksPerSecond = if (duration > 0) clicks.size.toDouble() / duration else 0.0
            if (clicksPerSecond > 10) {
                issues.add("Suspicious: Extreme click speed - ${String.format("%.1f", clicksPerSecond)} clicks/second")
                fraudScore += 0.5
            }
        }

        val isAnomalous = fraudScore > 0.3
        val recommendedAction = when {
            fraudScore > 0.7 -> "block"
            fraudScore > 0.5 -> "flag"
            fraudScore > 0.3 -> "warn"
            else -> "none"
        }

        return AnalysisResult(
            is_anomalous = isAnomalous,
            confidence = fraudScore.coerceIn(0.0, 1.0),
            fraud_probability = fraudScore,
            issues = issues,
            analysis = "Rule-based analysis: ${if (isAnomalous) "Suspicious patterns detected" else "No anomalies found"}",
            recommended_action = recommendedAction
        )
    }

    private fun calculateStdDev(numbers: List<Long>): Double {
        if (numbers.isEmpty()) return 0.0
        val mean = numbers.average()
        val variance = numbers.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance)
    }

    fun isModelReady(): Boolean = modelsReady.get()

    fun getModelName(): String = if (modelsReady.get()) modelName else "fallback-rules"

    fun clearCache() {
        analysisCache.clear()
        logger.info("Analysis cache cleared")
    }
}