import com.careerops.share.CareerOpsAction
import com.careerops.share.CareerOpsRequest
import com.careerops.share.CareerOpsRequestRenderer
import com.careerops.share.ShareParser

fun main() {
    fun check(name: String, condition: Boolean) {
        if (!condition) error("FAILED: $name")
        println("PASS: $name")
    }

    val linkedIn = ShareParser.parse(
        "Junior Data Engineer",
        "Take a look: https://www.linkedin.com/jobs/view/4458683736/?trackingId=abc"
    )
    check("LinkedIn source", linkedIn.source == "LinkedIn")
    check("LinkedIn job id", linkedIn.jobId == "4458683736")
    check("LinkedIn canonical URL", linkedIn.canonicalUrl == "https://www.linkedin.com/jobs/view/4458683736/")

    val indeed = ShareParser.parse(
        null,
        "https://www.indeed.com/viewjob?jk=abc123&from=appshareandroid)."
    )
    check("Indeed source", indeed.source == "Indeed")
    check("Indeed job id", indeed.jobId == "abc123")
    check("Indeed canonical URL", indeed.canonicalUrl == "https://www.indeed.com/viewjob?jk=abc123")

    val plain = ShareParser.parse(null, "Data analyst role copied from an email")
    check("Plain text source", plain.source == "Shared text")
    check("Plain text preserved", plain.rawSharedContent.contains("Data analyst role"))

    val custom = ShareParser.parse(null, "https://jobs.example.org/role/42?utm_source=share")
    check("Generic hostname", custom.source == "jobs.example.org")
    check("Generic tracking cleanup", custom.canonicalUrl == "https://jobs.example.org/role/42")

    val spoof = ShareParser.parse(null, "https://notlinkedin.com/jobs/123")
    check("Domain suffix spoof rejected", spoof.source == "notlinkedin.com")

    val request = CareerOpsRequest(
        action = CareerOpsAction.ANALYZE_BUILD_STORE,
        job = linkedIn
    )
    val text = CareerOpsRequestRenderer.toText(request)
    val json = CareerOpsRequestRenderer.toJson(request)

    check("CareerOps compatibility prefix", text.startsWith("Analyze this job using CareerOps:"))
    check("Action included", text.contains("action: ANALYZE_BUILD_STORE"))
    check("JSON schema included", json.contains("\"schema_version\": \"1.0\""))

    println("All v0.2 ShareParser smoke tests passed.")
}
