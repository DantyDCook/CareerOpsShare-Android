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

| ID | UI label |
| --- | --- |
| `ANALYZE` | Analyze |
| `ANALYZE_BUILD_STORE` | Analyze + Build & Store |
| `ANALYZE_BUILD_STORE_COVER_LETTER` | Analyze + Build & Store + Cover Letter |

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

and follows it with structured fields and preserved shared content.

## Destination is not part of the request

The Android destination is intentionally excluded from `CareerOpsRequest`. Destination is a delivery concern and must not alter the requested CareerOps operation.

## Model routing is not part of the Android request by default

The future CareerOps model broker should own worker-model selection. This allows routing policies and model availability to evolve independently from the Android release cycle.
