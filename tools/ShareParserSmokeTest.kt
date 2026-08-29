import com.careerops.share.ShareParser

fun main() {
    fun check(name: String, condition: Boolean) {
        if (!condition) error("FAILED: $name")
        println("PASS: $name")
    }

    val linkedIn = ShareParser.parse(
        "Junior Data Engineer",
        "Take a look: https://www.linkedin.com/jobs/view/4458683736/"
    )
    check("LinkedIn source", linkedIn.source == "LinkedIn")
    check("LinkedIn URL", linkedIn.url == "https://www.linkedin.com/jobs/view/4458683736/")
    check("CareerOps prefix", linkedIn.payload.startsWith("Analyze this job using CareerOps:"))

    val indeed = ShareParser.parse(null, "https://www.indeed.com/viewjob?jk=abc123).")
    check("Indeed source", indeed.source == "Indeed")
    check("Trailing punctuation stripped", indeed.url == "https://www.indeed.com/viewjob?jk=abc123")

    val plain = ShareParser.parse(null, "Data analyst role copied from an email")
    check("Plain text source", plain.source == "Shared text")
    check("Plain text preserved", plain.payload.contains("Data analyst role copied from an email"))

    val custom = ShareParser.parse(null, "https://jobs.example.org/role/42")
    check("Generic hostname", custom.source == "jobs.example.org")

    val spoof = ShareParser.parse(null, "https://notlinkedin.com/jobs/123")
    check("Domain suffix spoof rejected", spoof.source == "notlinkedin.com")

    println("All ShareParser smoke tests passed.")
}
