package com.lumen.search.data.governance

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ResourceGovernorTest {

    @Test
    fun `hasBudget returns true for new provider`() = runTest {
        val governor = ResourceGovernor()

        val hasBudget = governor.hasBudget("openalex")

        hasBudget.shouldBeTrue()
    }

    @Test
    fun `acquirePermit returns true when budget available`() = runTest {
        val governor = ResourceGovernor()

        val acquired = governor.acquirePermit("openalex")

        acquired.shouldBeTrue()
    }

    @Test
    fun `recordUsage tracks daily usage`() = runTest {
        val governor = ResourceGovernor()

        governor.acquirePermit("openalex")
        governor.recordUsage("openalex", 1)

        val stats = governor.getUsageStats("openalex")
        stats shouldNotBe null
        stats?.dailyUsage shouldBe 1
    }

    @Test
    fun `getUsageStats returns null for unknown provider`() = runTest {
        val governor = ResourceGovernor()

        val stats = governor.getUsageStats("unknown")

        stats shouldBe null
    }

    @Test
    fun `resetDailyCounters clears all usage`() = runTest {
        val governor = ResourceGovernor()

        governor.acquirePermit("openalex")
        governor.recordUsage("openalex", 100)
        governor.resetDailyCounters()

        val stats = governor.getUsageStats("openalex")
        stats?.dailyUsage shouldBe 0
    }

    @Test
    fun `respects per-provider rate limits`() = runTest {
        // Create a governor with a very low rate limit for testing
        val configs = mapOf(
            "test" to ProviderQuotaConfig(
                requestsPerSecond = 2,
                burstCapacity = 2,
                dailyLimit = null
            )
        )
        val governor = ResourceGovernor(configs)

        // Should be able to acquire burst capacity
        governor.acquirePermit("test").shouldBeTrue()
        governor.acquirePermit("test").shouldBeTrue()

        // After exhausting burst, no tokens available until refill
        governor.hasBudget("test").shouldBeFalse()

        // Wait for refill (real time)
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(600) // Wait 600ms - should refill ~1 token at 2/sec

        governor.hasBudget("test").shouldBeTrue()
    }

    @Test
    fun `daily limit enforcement works`() = runTest {
        val configs = mapOf(
            "test" to ProviderQuotaConfig(
                requestsPerSecond = 100,
                burstCapacity = 100,
                dailyLimit = 2
            )
        )
        val governor = ResourceGovernor(configs)

        // First two requests succeed
        governor.acquirePermit("test").shouldBeTrue()
        governor.recordUsage("test", 1)
        governor.acquirePermit("test").shouldBeTrue()
        governor.recordUsage("test", 1)

        // Third request should fail due to daily limit
        governor.hasBudget("test").shouldBeFalse()
        governor.acquirePermit("test").shouldBeFalse()
    }

    @Test
    fun `uses default config for known providers`() = runTest {
        val governor = ResourceGovernor()

        // OpenAlex should have default config
        governor.hasBudget("openalex").shouldBeTrue()
        governor.hasBudget("semanticscholar").shouldBeTrue()
        governor.hasBudget("crossref").shouldBeTrue()
        governor.hasBudget("arxiv").shouldBeTrue()
    }

    @Test
    fun `default configs have sensible rate limits`() {
        val defaults = ResourceGovernor.defaultConfigs()

        // OpenAlex: 10 req/sec
        defaults["openalex"]?.requestsPerSecond shouldBe 10

        // Semantic Scholar: 5 req/sec (without API key)
        defaults["semanticscholar"]?.requestsPerSecond shouldBe 5

        // Crossref: 50 req/sec (polite pool)
        defaults["crossref"]?.requestsPerSecond shouldBe 50

        // ArXiv: 1 req/sec (very strict)
        defaults["arxiv"]?.requestsPerSecond shouldBe 1
    }

    @Test
    fun `semantic scholar has daily limit without API key`() {
        val defaults = ResourceGovernor.defaultConfigs()

        defaults["semanticscholar"]?.dailyLimit shouldBe 5_000
    }

    @Test
    fun `crossref has no daily limit`() {
        val defaults = ResourceGovernor.defaultConfigs()

        defaults["crossref"]?.dailyLimit shouldBe null
    }
}

class TokenBucketTest {

    @Test
    fun `hasTokens returns true with full bucket`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 10,
            dailyLimit = null
        )
        val bucket = TokenBucket(config)

        bucket.hasTokens().shouldBeTrue()
    }

    @Test
    fun `acquire consumes token`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 2,
            dailyLimit = null
        )
        val bucket = TokenBucket(config)

        bucket.acquire().shouldBeTrue()
        bucket.acquire().shouldBeTrue()

        // After burst capacity, tokens should be lower
        val stats = bucket.getStats()
        stats.tokensAvailable shouldBeLessThan 2.0
    }

    @Test
    fun `recordUsage tracks daily count`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 10,
            dailyLimit = 100
        )
        val bucket = TokenBucket(config)

        bucket.recordUsage(5)

        bucket.getStats().dailyUsage shouldBe 5
    }

    @Test
    fun `resetDaily clears usage`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 10,
            dailyLimit = 100
        )
        val bucket = TokenBucket(config)

        bucket.recordUsage(50)
        bucket.resetDaily()

        bucket.getStats().dailyUsage shouldBe 0
    }

    @Test
    fun `daily limit prevents token acquisition`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 10,
            dailyLimit = 1
        )
        val bucket = TokenBucket(config)

        bucket.acquire().shouldBeTrue()
        bucket.recordUsage(1)

        // Daily limit exhausted
        bucket.acquire().shouldBeFalse()
    }

    @Test
    fun `tokens refill over time`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 100, // 100 tokens/sec = 10 tokens per 100ms
            burstCapacity = 2,
            dailyLimit = null
        )
        val bucket = TokenBucket(config)

        // Exhaust tokens
        bucket.acquire()
        bucket.acquire()

        // Verify tokens are exhausted
        bucket.hasTokens().shouldBeFalse()

        // Use real time delay for refill to work with Clock.System
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(150) // Wait 150ms - should refill ~15 tokens at 100/sec

        // hasTokens() triggers refill internally
        bucket.hasTokens().shouldBeTrue()
    }

    @Test
    fun `tokens do not exceed burst capacity`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 100,
            burstCapacity = 5,
            dailyLimit = null
        )
        val bucket = TokenBucket(config)

        // Wait for potential over-refill
        delay(200.milliseconds)

        val stats = bucket.getStats()
        stats.tokensAvailable shouldBeLessThan 6.0  // Should not exceed burst + 1
    }

    @Test
    fun `getStats returns accurate information`() = runTest {
        val config = ProviderQuotaConfig(
            requestsPerSecond = 10,
            burstCapacity = 15,
            dailyLimit = 1000
        )
        val bucket = TokenBucket(config)

        bucket.recordUsage(50)

        val stats = bucket.getStats()
        stats.requestsPerSecond shouldBe 10
        stats.dailyLimit shouldBe 1000
        stats.dailyUsage shouldBe 50
    }
}

