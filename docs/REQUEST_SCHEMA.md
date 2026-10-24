# CareerOps Request Schema v1.0

## Purpose

`CareerOpsRequest` is the transport-neutral handoff contract emitted by CareerOps Share.

The same logical request can be rendered as structured text for a conversational AI application or JSON for a future CareerOps gateway, webhook, API, or other machine client.

## Logical model

```text
CareerOpsRequest
├── schemaVersion
├── action
└── job
    ├── source
    ├── sourceId
    ├── jobId
    ├── originalUrl
    ├── canonicalUrl
    ├── subject
    ├── rawSharedContent
    └── wasTruncated
```

## Supported actions in v0.2

| ID | Required fulfillment |
| --- | --- |
| `ANALYZE` | Analyze / job fit only |
| `ANALYZE_BUILD_STORE` | Analyze + required tailored resume (DOCX and PDF) + verified storage |
| `ANALYZE_BUILD_STORE_COVER_LETTER` | Full Application: analyze + required tailored resume and cover letter (each DOCX and PDF) + verified storage |

## Example JSON

```json
{
  "schema_version": "1.0",
  "action": "ANALYZE_BUILD_STORE",
  "job": {
    "source": "LinkedIn",
    "source_id": "linkedin",
    "job_id": "4453238792",
    "original_url": "https://www.linkedin.com/jobs/view/4453238792/?trackingId=example",
    "canonical_url": "https://www.linkedin.com/jobs/view/4453238792/",
    "subject": "AI Engineer",
    "was_truncated": false,
    "raw_shared_content": "AI Engineer\nhttps://www.linkedin.com/jobs/view/4453238792/?trackingId=example"
  }
}
```

## Compatibility text

For conversational clients, the request begins with:

```text
Analyze this job using CareerOps:
```

and follows it with structured fields and preserved shared content. For build actions, the renderer also adds an **Execution requirements** section ahead of the shared source content. The JSON renderer includes the same wording as an optional top-level `execution_instructions` string (escaped as JSON), leaving all existing required v1.0 fields and action IDs unchanged. The guidance is derived from `action` during rendering; it is not a persisted `CareerOpsRequest` model property.

The additional guidance is a **compatibility-oriented client hint** for conversational destinations, not a claim that the Android client runs a renderer or has completed persistence. Parsers that only accept an exact list of JSON keys should be updated to tolerate this optional extension; the canonical action semantics belong to CareerOps Engine. Neither a generated request nor an analysis response alone verifies that the artifacts were created.

### Build completion rules

- `ANALYZE_BUILD_STORE`: fit assessment plus tailored resume in **DOCX and PDF**; cover letter is not required.
- `ANALYZE_BUILD_STORE_COVER_LETTER`: fit assessment plus tailored resume **and** cover letter, each in **DOCX and PDF**.
- Use canonical validated evidence, audit the documents, and verify writes to the configured Workspace and artifact store when authorized and accessible.
- State `PARTIAL` with missing formats, inaccessible sources or storage, and next actions if requested outputs cannot be verified. Never return invented links or claim that draft text is a saved file.
- Neither build action grants authorization to submit an application or change external accounts.

For authoritative action fulfillment and response behavior, see the Engine's `docs/workflows/CAREEROPS_SHARE_ACTIONS.md`.

## Destination is not part of the request

The Android destination is intentionally excluded from `CareerOpsRequest`. Destination is a delivery concern and must not alter the requested CareerOps operation.

## Model routing is not part of the Android request by default

The future CareerOps model broker should own worker-model selection. This allows routing policies and model availability to evolve independently from the Android release cycle.
