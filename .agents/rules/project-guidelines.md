# Project Guidelines: Property Bill Automation (cohort-team-2026-the-declared-royalty)

> **Sources:** the cohort brief, `My Notes`, `Project Notes` (Discord announcements), `Supabase`, `TODO`, `Talking Points`, the ERD, Swagger and MinIO screenshots, the mock invoices, and the repo README and code.
> **Gaps:** the duplicate-detection criteria in `My Notes` came through blank in the export. The mock invoice PDFs were only partly read (vendor names come from file names plus the Alder Creek sample). Items marked *(inferred)* come from the code or schema. Items marked **OPEN** are unresolved questions from the team's "Questions for Leaders" list.

## Project Domain & Context

A bill-automation platform for **property-management portfolios**. A real CTO reported that staff spend about two full days a month working out what each bill is, which property it belongs to, how to pay it, and whether it should be paid at all, across hundreds of properties.

- **Terminology:**
  - *Vendor*: the external payee (for example Alder Creek Landscaping, Brightwater Water & Sewer).
  - *Customer*: the internal property-management entity that pays.
  - *Property*: the physical asset or service location (for example 125 Industrial Way, Springfield, IL 62701).
  - *Invoice*: the (often unstructured) statement from a vendor for a specific property and billing cycle.
- **Domain assumption ("Interpretation B"):** a single Customer holds one enterprise portfolio. Each property has its own service accounts with vendors (grounds care, water/sewer, HVAC, pest control). All 10 sample invoices share one address but have distinct "billed to" names. **OPEN:** whether the samples are 10 tenants of one property (Interpretation A) or 10 service accounts held by one entity (Interpretation B). **OPEN:** whether future test batches add more addresses.
- **Program context:** a 6-week DSD cohort project. The team stack is fixed: **Java, Spring, React**. Do not adopt new stacks. Ten fake vendors and ten fake invoices are provided for testing.
- **Team and process:** backend engineers work in Java and Spring Boot. Four frontend/QA members build the React app. Daily standups must be meaningful, and blockers are raised in the team channels.
- **MVP discipline:** define the MVP early and do not deviate from it or sacrifice it. Planned deliverables are a PRD, a system architecture document, an ERD, API docs, and an AI Usage Document. Detailed security and test plans may be deferred under time pressure.

### AI timeline (strict)
- **Weeks 1 to 2:** no generative AI and no AI coding assistants. Build a **completely deterministic** pipeline. Parse with programmatic text extraction (Apache PDFBox) and strict regex or rule-based field mapping.
- **Weeks 3 to 6:** introduce probabilistic components (LLMs, agents, OCR) only to turn noisy or unstructured invoices into structured payloads. These then feed the deterministic pipeline.
- AI is used sparingly, only where deterministic code cannot do the job. Never use it for duplicate detection, approvals, or payment decisions.

## Core Feature Requirements & Scope

The pipeline has five stages. The Friday milestone is an architecture diagram that every team member can explain.

1. **Intake:** upload a PDF (or image), or enter a bill manually. Save the file to BLOB storage, compute its SHA-256, and set status `INGESTED`. Email and other advanced intake are out of scope.
2. **Parsing:** extract text with PDFBox and map fields with rules. Fields: date, amount, account number, vendor, due date, billing period, vendor invoice number. Flag missing fields.
3. **Entity matching:** match the invoice to an active `property_vendor_mapping`. Route unmapped invoices to the review queue.
4. **Validation:** duplicate checks, amount sanity checks, and checks against prior payments. Flag missing information and unusual amounts.
5. **Approval and payment:**
   - An auto-approve rule applies where allowed.
   - Anything else goes to a manual review queue.
   - Role-based approvals are required. **OPEN:** the roles (for example Property Manager vs Finance Director) and dollar thresholds.
   - Payment is an **internal ledger state transition** (`APPROVED` to `PAID`). It records the payment method (ACH, check, card) and writes an immutable audit log. The default scope is that no real money moves through external portals. **OPEN:** whether to call the demo vendor portals. The README still mentions Stripe in test mode, and that conflicts with this default.
6. **Exception handling:** unknown vendors, inconsistent invoices, missing or conflicting data, and failed payments stop safely and go to a human.
7. **History and auditability:** anyone can see where a bill is, what was decided, and why.

**Planned API** (contract-first): `POST /api/v1/invoices/upload`, `GET /api/v1/invoices`, and so on. **Implemented so far:** `GET` and `POST /api/v1/properties`, and `GET /api/v1/properties/{propertyId}` (404 when not found). Vendor and User controllers also exist. **OPEN:** whether the cohort provides a seed dataset for properties, vendors and account numbers. Otherwise the team designs and seeds its own master data.

## Data Models & State Management

Relational database (PostgreSQL 16, Flyway migrations). Raw PDFs go in MinIO. The database stores only the file pointer URL, the SHA-256 and the extracted metadata.

| Table | Key fields |
|---|---|
| `properties` | `property_uuid` (PK), `name`, `street_address`, `city`, `state`, `zip_code`, `status`, timestamps |
| `vendors` | `vendor_uuid` (PK), `vendor_name`, `billing_phone_number`, `status`, `payment_url`, `payment_method`, timestamps. Notes describe it as the master list of approved vendors with a default payment method and portal URL. |
| `property_vendor_mapping` | `mapping_uuid`, `vendor_uuid`, `property_uuid`, `vendor_account_number`, `status`, `auto_approval_enabled`, timestamps. Many-to-many junction: the authorized vendor contract per property. |
| `invoices` | `invoice_uuid` (PK), `vendor_supplied_invoice_id`, `mapping_uuid`, `billing_period_start/end`, `invoice_creation_date`, `amount numeric(10,2)`, `due_date`, `invoice_status`, `failure_reason`, `blob_file_url`, `blob_file_sha256`, timestamps |
| `users` | `user_uuid`, `user_name`, `email`, `role`, `is_active`, timestamps |
| `audit_logs` | `audit_uuid`, `invoice_uuid`, `user_uuid`, `action_type`, `action_taken`, `from_state`, `to_state`, `notes`, timestamps. Append-only: every state transition, actor (user or system), timestamp and failure reason. |

**State management:** invoice statuses include `INGESTED` and `APPROVED` through `PAID`, plus review and exception states. *(The full list is inferred.)* Each transition is recorded in `audit_logs`.

**Identifiers:** vendor account numbers (for example `2000456700`) and vendor invoice numbers (for example `BILL-1001`) are **untrusted external metadata**. They are never used as primary keys. The system generates its own immutable surrogate key (UUID) for every invoice.

## Constraints & Edge Cases

- **Duplicates must be caught deterministically.** The rule matches against existing records. The file SHA-256 gives instant duplicate-file detection. Also compare vendor, account and vendor invoice reference. *(The exact criteria were blank in the source notes. Confirm them.)*
- **Never let AI make a financial decision.** Approvals, duplicate checks and payment are rules in code.
- **Match before action:** nothing happens to a bill until it is tied to the correct property and account.
- **Fail safe:** low confidence, missing data or a conflict means stop, record `failure_reason`, and send to human review.
- **Template variance:** the ten vendors use different invoice layouts, billed-to names, account numbers, due dates, statuses and pay links. Do not hard-code one template.
- **Per-mapping settings:** a property can have a different vendor, account, portal, payment method, approval rule and due date for each service. Model these on the mapping, not globally.
- **Money and time:** amounts are `numeric(10,2)`. Timestamps carry a time zone.
- **Contract-first workflow:** change the OpenAPI spec/fragments under `backend/src/main/resources/openapi/` first (root `openapi.yaml`, plus `paths/` and `components/`), then run `./mvnw compile` to regenerate, then implement the generated interfaces. Schema changes need a new Flyway migration (`V<n>__desc.sql`), and applied migrations are never edited.
- **Java conventions from the team notes:** generate DTOs as Java records (`useRecords=true`) and map with MapStruct. Entities use Lombok `@Getter`, `@Setter` and `@NoArgsConstructor` only (no `@Builder`). Keep business defaults, such as status `ACTIVE`, explicit in code.
- **Database workflow guidance (from the Supabase doc):** never work directly on production. Treat migrations like commits. Use feature branches named `feature/<name>`. Coordinate before any deploy. Never leak or commit service keys. **If Supabase is adopted:** enable Row Level Security on every table. The current repo uses Docker Postgres with Flyway, so Supabase is **OPEN**.
- **Security:** secrets stay in `.env`. Access is role-based through `users.role`. Uploaded files are untrusted.
- **Repo inconsistencies:**
  - `frontend/` has a stray `node-modules` directory.
- **Success criterion:** the team can explain why AI was and wasn't used, how uncertainty was handled, and how financial decisions were protected.
