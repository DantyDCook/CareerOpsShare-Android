package com.careerops.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareParserTest {

    @Test
    fun linkedinShareExtractsJobIdAndCanonicalUrl() {
        val intake = ShareParser.parse(
            "AI Engineer",
            "https://www.linkedin.com/jobs/view/4453238792/?trackingId=abc&utm_source=test"
        )

        assertEquals("LinkedIn", intake.source)
        assertEquals("linkedin", intake.sourceId)
        assertEquals("4453238792", intake.jobId)
        assertEquals("https://www.linkedin.com/jobs/view/4453238792/", intake.canonicalUrl)
    }

    @Test
    fun linkedinSlugStillExtractsTrailingNumericId() {
        val intake = ShareParser.parse(
            null,
            "https://www.linkedin.com/jobs/view/ai-engineer-at-example-4458683736/"
        )

        assertEquals("4458683736", intake.jobId)
        assertEquals("https://www.linkedin.com/jobs/view/4458683736/", intake.canonicalUrl)
    }

    @Test
    fun indeedShareExtractsJkAndDropsShareNoise() {
        val intake = ShareParser.parse(
            null,
            "https://www.indeed.com/viewjob?jk=9adc659af49e63a8&from=appshareandroid&utm_source=copy"
        )

        assertEquals("Indeed", intake.source)
        assertEquals("9adc659af49e63a8", intake.jobId)
        assertEquals("https://www.indeed.com/viewjob?jk=9adc659af49e63a8", intake.canonicalUrl)
    }

    @Test
    fun genericUrlDropsCommonTrackingButKeepsRealQuery() {
        val intake = ShareParser.parse(
            null,
            "https://jobs.example.org/role/42?department=data&utm_source=share&gclid=abc"
        )

        assertEquals("https://jobs.example.org/role/42?department=data", intake.canonicalUrl)
    }

    @Test
    fun trailingPunctuationIsRemoved() {
        val intake = ShareParser.parse(
            null,
            "Apply: https://www.indeed.com/viewjob?jk=abc123)."
        )

        assertEquals("abc123", intake.jobId)
        assertEquals("https://www.indeed.com/viewjob?jk=abc123", intake.canonicalUrl)
    }

    @Test
    fun plainTextShareRemainsValid() {
        val intake = ShareParser.parse(null, "Data analyst role copied from an email")

        assertEquals("Shared text", intake.source)
        assertEquals("shared_text", intake.sourceId)
        assertNull(intake.jobId)
        assertNull(intake.canonicalUrl)
        assertTrue(intake.rawSharedContent.contains("Data analyst role"))
    }

    @Test
    fun lookalikeDomainIsNotLinkedIn() {
        val intake = ShareParser.parse(null, "https://notlinkedin.com/jobs/123")

        assertEquals("notlinkedin.com", intake.source)
        assertEquals("notlinkedin.com", intake.sourceId)
    }

    @Test
    fun manualUrlInputBuildsStructuredCareerOpsRequest() {
        val intake = ShareParser.parse(
            null,
            "https://www.linkedin.com/jobs/view/4457534369/?trackingId=manual"
        )
        val request = CareerOpsRequest(
            action = CareerOpsAction.ANALYZE_BUILD_STORE,
            job = intake
        )

        val rendered = CareerOpsRequestRenderer.toText(request)

        assertEquals("LinkedIn", intake.source)
        assertEquals("4457534369", intake.jobId)
        assertEquals("https://www.linkedin.com/jobs/view/4457534369/", intake.canonicalUrl)
        assertTrue(rendered.contains("schema_version: 1.0"))
        assertTrue(rendered.contains("action: ANALYZE_BUILD_STORE"))
    }

    @Test
    fun requestRendererPreservesCompatibilityPromptAndAction() {
        val intake = ShareParser.parse(null, "https://www.linkedin.com/jobs/view/4453238792/")
        val request = CareerOpsRequest(
            action = CareerOpsAction.ANALYZE_BUILD_STORE,
            job = intake
        )

        val rendered = CareerOpsRequestRenderer.toText(request)

        assertTrue(rendered.startsWith("Analyze this job using CareerOps:"))
        assertTrue(rendered.contains("action: ANALYZE_BUILD_STORE"))
        assertTrue(rendered.contains("job_id: 4453238792"))
    }

    @Test
    fun jsonRendererProducesStructuredContract() {
        val intake = ShareParser.parse(
            "Role \"A\"",
            "https://www.indeed.com/viewjob?jk=abc123"
        )
        val request = CareerOpsRequest(
            action = CareerOpsAction.ANALYZE,
            job = intake
        )

        val json = CareerOpsRequestRenderer.toJson(request)

        assertTrue(json.contains("\"schema_version\": \"1.0\""))
        assertTrue(json.contains("\"action\": \"ANALYZE\""))
        assertTrue(json.contains("\"job_id\": \"abc123\""))
        assertTrue(json.contains("Role \\\"A\\\""))
        assertFalse(json.contains("\"destination\""))
    }
}
