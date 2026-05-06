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
import java.net.ConnectException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.springframework.http.HttpMethod

@Service
class ActionAnalysisService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Value("\${ollama.url:http://ollama:11434}")
    private lateinit var ollamaUrl: String

    @Value("\${ai.model.name:tinyllama:latest}")
    private lateinit var modelName: String

    @Value("\${ai.enabled:true}")
    private var aiEnabled: Boolean = true

    @Value("\${ai.fallback.no-model:true}")
    private var fallbackWhenNoModel: Boolean = true

    private val modelsReady = AtomicBoolean(false)
    private val serviceAvailable = AtomicBoolean(false)
    private val analysisCache = ConcurrentHashMap<String, CachedAnalysis>()
    private var pullAttempts = 0
    private val maxPullAttempts = 5

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
            "num_predict" to 300,
            "repeat_penalty" to 1.1,
            "num_ctx" to 2048
        )
    )

    @PostConstruct
    fun initialize() {
        if (!aiEnabled) {
            logger.info("AI analysis is disabled")
            return
        }

        scope.launch {
            delay(5000)
            checkServiceAndModel()
        }
    }

    private suspend fun checkServiceAndModel() {
        if (modelsReady.get()) return

        if (!isServiceReachable()) {
            logger.warn("Ollama service not reachable at $ollamaUrl")
            return
        }

        serviceAvailable.set(true)

        if (isModelAvailable()) {
            logger.info("Model already available: $modelName")
            modelsReady.set(true)
            warmUpModel()
            return
        }

        if (pullAttempts >= maxPullAttempts) {
            logger.warn("Max pull attempts ($maxPullAttempts) reached. Skipping model pull.")
            return
        }

        pullAttempts++
        logger.info("Attempt $pullAttempts/$maxPullAttempts to pull model $modelName")
        startPull()
    }

    private fun isServiceReachable(): Boolean {
        return try {
            val url = "$ollamaUrl/api/tags"
            restTemplate.getForObject(url, Map::class.java)
            true
        } catch (e: ConnectException) {
            logger.debug("Ollama service not reachable: ${e.message}")
            false
        } catch (e: Exception) {
            logger.debug("Service check failed: ${e.message}")
            false
        }
    }

    private fun isModelAvailable(): Boolean {
        if (!serviceAvailable.get()) return false

        return try {
            val url = "$ollamaUrl/api/tags"
            val response = restTemplate.getForObject(url, Map::class.java)
            val models = response?.get("models") as? List<*> ?: emptyList<Any?>()
            models.any { model ->
                val name = (model as? Map<*, *>)?.get("name")?.toString() ?: ""
                name.contains(modelName.split(":")[0], ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun startPull() {
        try {
            val url = java.net.URI("$ollamaUrl/api/pull")
            val request = mapOf("name" to modelName, "stream" to false)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val requestBody = objectMapper.writeValueAsString(request)

            restTemplate.execute(url, HttpMethod.POST,
                { requestCallback ->
                    requestCallback.headers.putAll(headers)
                    requestCallback.body.write(requestBody.toByteArray())
                },
                { responseCallback ->
                    responseCallback.body?.bufferedReader()?.readText()
                    null
                }
            )

            scope.launch {
                waitForModelWithRetry()
            }
        } catch (e: Exception) {
            logger.error("Failed to start pull: ${e.message}")
        }
    }

    private suspend fun waitForModelWithRetry() {
        var waitTime = 0
        val maxWaitSeconds = 180

        while (waitTime < maxWaitSeconds && !modelsReady.get()) {
            delay(5000)
            waitTime += 5

            if (isModelAvailable()) {
                logger.info("Model ready after ${waitTime} seconds")
                modelsReady.set(true)
                warmUpModel()
                return
            }

            if (waitTime % 30 == 0) {
                logger.info("Waiting for model download... (${waitTime}s elapsed)")
            }
        }

        if (!modelsReady.get()) {
            logger.warn("Model download incomplete after ${maxWaitSeconds}s")
        }
    }

    private suspend fun warmUpModel() {
        try {
            withTimeout(5000) {
                val warmupPrompt = "Respond with OK"
                val request = OllamaGenerateRequest(
                    model = modelName,
                    prompt = warmupPrompt
                )
                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }
                val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
                restTemplate.postForObject("$ollamaUrl/api/generate", entity, Map::class.java)
                logger.info("Model warmed up and ready")
            }
        } catch (e: Exception) {
            logger.warn("Model warmup failed: ${e.message}")
        }
    }

    suspend fun analyzeActions(
        actions: List<Action>,
        userId: Long,
        forceFresh: Boolean = false
    ): AnalysisResult {
        val startTime = System.currentTimeMillis()

        val actionHash = actions.take(100).hashCode()
        if (!forceFresh) {
            val cached = analysisCache["$userId-$actionHash"]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < 300000) {
                return cached.result
            }
        }

        val result = if (!aiEnabled || !modelsReady.get()) {
            ruleBasedAnalysis(actions, userId)
        } else {
            try {
                withTimeout(15000) {
                    llmBasedAnalysis(actions, userId)
                }
            } catch (e: TimeoutCancellationException) {
                ruleBasedAnalysis(actions, userId).copy(
                    analysis = "Analysis timeout, using rule-based fallback"
                )
            } catch (e: Exception) {
                ruleBasedAnalysis(actions, userId).copy(
                    analysis = "Analysis failed: ${e.message}, using fallback"
                )
            }
        }

        val finalResult = result.copy(processing_time_ms = System.currentTimeMillis() - startTime)
        analysisCache["$userId-$actionHash"] = CachedAnalysis(
            result = finalResult,
            timestamp = System.currentTimeMillis(),
            actionIdsHash = actionHash
        )

        if (analysisCache.size > 100) {
            val oldEntries = analysisCache.entries.filter {
                System.currentTimeMillis() - it.value.timestamp > 3600000
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

        return try {
            val response = restTemplate.postForObject(url, entity, Map::class.java)
            val responseText = response?.get("response") as? String ?: ""
            parseAnalysisResponse(responseText, actions, userId)
        } catch (e: RestClientException) {
            throw e
        }
    }

    private fun buildAnalysisPrompt(actions: List<Action>, userId: Long): String {
        val metrics = calculateActionMetrics(actions)

        val recentActions = actions.takeLast(30).joinToString("\n") { action ->
            when (action) {
                is AppAction -> "  - App switch: ${action.app_name}"
                is MouseAction -> {
                    if (action.is_click) "  - Click at (${action.delta_x}, ${action.delta_y})"
                    else "  - Mouse move to (${action.delta_x}, ${action.delta_y})"
                }
                is KeyboardAction -> "  - Key pressed: ${action.keyboard_key}"
                else -> "  - Unknown action"
            }
        }

        return """
You are a fraud detection system. Analyze user behavior and return ONLY valid JSON.

User $userId behavior:
- Total actions: ${actions.size}
- Mouse clicks: ${metrics.clicks}
- Mouse movements: ${metrics.moves}
- Keyboard inputs: ${metrics.keys}
- App switches: ${metrics.apps}
- Duration: ${metrics.durationSecs} seconds
- Click speed: ${"%.1f".format(metrics.clicksPerSecond)} clicks/second

Recent actions:
$recentActions

Return ONLY valid JSON:
{
    "is_anomalous": true/false,
    "confidence": 0.0-1.0,
    "fraud_probability": 0.0-1.0,
    "issues": ["issue1", "issue2"],
    "analysis": "brief explanation",
    "recommended_action": "none/warn/flag/block"
}
""".trimIndent()
    }

    private fun parseAnalysisResponse(responseText: String, actions: List<Action>, userId: Long): AnalysisResult {
        return try {
            var cleanResponse = responseText.trim()
            cleanResponse = cleanResponse.replace("```json", "").replace("```", "")

            val jsonStart = cleanResponse.indexOf('{')
            val jsonEnd = cleanResponse.lastIndexOf('}') + 1
            val jsonString = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleanResponse.substring(jsonStart, jsonEnd)
            } else {
                cleanResponse
            }

            val result = objectMapper.readValue<AnalysisResult>(jsonString)

            AnalysisResult(
                is_anomalous = result.is_anomalous,
                confidence = result.confidence.coerceIn(0.0, 1.0),
                fraud_probability = result.fraud_probability.coerceIn(0.0, 1.0),
                issues = result.issues.take(5),
                analysis = result.analysis.ifEmpty {
                    if (result.is_anomalous) "Suspicious patterns detected" else "Normal behavior"
                },
                recommended_action = when (result.recommended_action.lowercase()) {
                    "block" -> "block"
                    "flag" -> "flag"
                    "warn" -> "warn"
                    else -> "none"
                }
            )
        } catch (e: Exception) {
            val fallback = ruleBasedAnalysis(actions, userId)
            fallback.copy(analysis = "AI response parsing failed: ${e.message}")
        }
    }

    private data class ActionMetrics(
        val clicks: Int,
        val moves: Int,
        val keys: Int,
        val apps: Int,
        val durationSecs: Long,
        val clicksPerSecond: Double
    )

    private fun calculateActionMetrics(actions: List<Action>): ActionMetrics {
        val clicks = actions.filterIsInstance<MouseAction>().count { it.is_click }
        val moves = actions.filterIsInstance<MouseAction>().count { !it.is_click }
        val keys = actions.filterIsInstance<KeyboardAction>().size
        val apps = actions.filterIsInstance<AppAction>().size

        val timestamps = actions.map { it.performedAt.toEpochSecond(java.time.ZoneOffset.UTC) }
        val durationSecs = if (timestamps.size > 1) timestamps.last() - timestamps.first() else 0L
        val clicksPerSecond = if (durationSecs > 0) clicks.toDouble() / durationSecs else 0.0

        return ActionMetrics(clicks, moves, keys, apps, durationSecs, clicksPerSecond)
    }

    private fun ruleBasedAnalysis(actions: List<Action>, userId: Long): AnalysisResult {
        val metrics = calculateActionMetrics(actions)

        val issues = mutableListOf<String>()
        var fraudScore = 0.0

        if (metrics.clicksPerSecond > 5) {
            issues.add("High click speed: ${"%.1f".format(metrics.clicksPerSecond)} clicks/second")
            fraudScore += 0.3
        }
        if (metrics.clicksPerSecond > 10) {
            issues.add("Extreme click speed: possible bot automation")
            fraudScore += 0.5
        }

        if (metrics.keys == 0 && metrics.clicks > 30) {
            issues.add("No keyboard activity with ${metrics.clicks} mouse clicks")
            fraudScore += 0.25
        }

        if (metrics.apps > 30 && metrics.clicks + metrics.keys < 50) {
            issues.add("Excessive app switching (${metrics.apps} times) with minimal activity")
            fraudScore += 0.2
        }

        if (metrics.durationSecs < 10 && metrics.clicks + metrics.keys > 80) {
            issues.add("Suspicious: ${metrics.clicks + metrics.keys} actions in ${metrics.durationSecs} seconds")
            fraudScore += 0.4
        }

        if (metrics.clicks > 0 && metrics.moves == 0 && metrics.clicks > 20) {
            issues.add("All clicks without mouse movement - possible automated clicking")
            fraudScore += 0.35
        }

        val isAnomalous = fraudScore > 0.3
        val recommendedAction = when {
            fraudScore > 0.7 -> "block"
            fraudScore > 0.5 -> "flag"
            fraudScore > 0.3 -> "warn"
            else -> "none"
        }

        val analysisText = if (isAnomalous) {
            "Rule-based analysis detected suspicious patterns: ${issues.joinToString("; ")}"
        } else {
            "Rule-based analysis found no significant anomalies"
        }

        return AnalysisResult(
            is_anomalous = isAnomalous,
            confidence = fraudScore.coerceIn(0.0, 1.0),
            fraud_probability = fraudScore,
            issues = issues,
            analysis = analysisText,
            recommended_action = recommendedAction
        )
    }

    fun isModelReady(): Boolean = modelsReady.get()
    fun getModelName(): String = if (modelsReady.get()) modelName else "fallback-rules"
    fun clearCache() {
        analysisCache.clear()
        logger.info("Analysis cache cleared")
    }
}