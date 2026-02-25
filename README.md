# InvoiceAI — Backend API

AI-powered invoice & expense management platform for freelancers and small businesses.

## Tech Stack

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.5
- **Auth:** Spring Security + JWT (internal, no external auth service)
- **ORM:** Spring Data JPA + Hibernate
- **Database:** PostgreSQL 16 (Neon serverless)
- **Migrations:** Flyway
- **File Storage:** Cloudflare R2 (S3-compatible)
- **AI:** Swappable — Gemini Flash (default/free) / Claude / OpenAI via strategy pattern
- **Email:** Resend (transactional)
- **Build:** Maven
- **Deployment:** Render (Docker)

## Features (16 API Modules)

### Core
- **Auth** — Register, login, JWT access/refresh tokens, password reset
- **Invoices** — Upload to R2, trigger AI extraction, status tracking
- **AI Extraction** — OCR invoices to structured JSON (vendor, amount, date, tax, line items, confidence scores)
- **Expenses** — Full CRUD with approval workflow (NEEDS_REVIEW → APPROVED/REJECTED)
- **Categories** — Custom + default categories with color management
- **Dashboard** — Spend by category, monthly trends, top vendors, summary stats

### Advanced
- **Vendor Directory** — Auto-populated from expenses, set default categories
- **Expense Policies** — Define rules (amount limits, required fields, category restrictions), auto-flag violations
- **Budget Tracking** — Monthly limits per category/overall, 80%/100% alerts, progress calculation
- **Recurring Expenses** — Templates with frequency (weekly/monthly/quarterly/yearly), auto-creation scheduler
- **Duplicate Detection** — Flags potential duplicate expenses (same vendor + amount + date)
- **Tax Reports** — Tax summary aggregation by category and vendor with date range
- **Audit Logs** — Who changed what, when (entity type, action, changes, IP address)
- **CSV Bulk Import** — Import multiple expenses at once with per-row error reporting
- **Export to Email** — Send expense report CSV to user's email via Resend
- **AI Assistant** — Chat endpoint powered by Gemini for expense insights

## Architecture

### Swappable AI (Strategy Pattern)

```
AIExtractionService (interface)
├── GeminiExtractionService     ← default (free: 1,500 req/day)
├── ClaudeExtractionService     ← Anthropic API
└── OpenAIExtractionService     ← GPT-4o Vision
```

Switch provider with one env var:
```yaml
app.ai.provider=gemini   # or "claude" or "openai"
```

### Multi-Tenancy

Shared database with `organization_id` discriminator column on all business tables. Thread-local `TenantContext` set from JWT + `X-Organization-Id` header. PostgreSQL RLS policies as defense-in-depth.

### BFF Pattern

Frontend (Next.js) calls this backend through its own API routes — tokens are injected server-side from httpOnly cookies. The browser never calls this backend directly.

## Project Structure

```
src/main/java/com/invoiceai/
├── config/          # SecurityConfig, AIProviderConfig, S3Config, CorsConfig
├── security/        # JwtTokenProvider, JwtAuthenticationFilter, TenantContext
├── controller/      # 14 REST controllers
├── service/         # 18 services (business logic)
├── repository/      # 13 JPA repositories
├── model/           # 14 JPA entities + 7 enums
├── dto/             # Request/response DTOs (16 requests, 20 responses)
├── exception/       # GlobalExceptionHandler + custom exceptions
├── mapper/          # Entity ↔ DTO mappers
└── scheduler/       # Recurring expense auto-creation, monthly resets
```

## Getting Started

```bash
# Prerequisites: Java 21, Maven, PostgreSQL

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with Docker
docker build -t invoiceai-api .
docker run -p 8080:8080 --env-file .env invoiceai-api
```

## Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://...
JWT_SECRET=your-secret-key
APP_AI_PROVIDER=gemini
GEMINI_API_KEY=...
S3_BUCKET=invoiceai-bucket
S3_ACCESS_KEY=...
S3_SECRET_KEY=...
S3_ENDPOINT=https://...r2.cloudflarestorage.com
RESEND_API_KEY=...
```

## Database Migrations

10+ Flyway migrations covering:
- Users, organizations, organization members
- Categories (with default seeding function)
- Invoices, expenses, expense line items
- Vendors, policies, policy violations
- Budgets, recurring expenses
- Audit logs, refresh tokens
- Row-Level Security policies

## Related

- **Frontend:** [invoiceai-web](https://github.com/it-harsh/invoiceai-web) — Next.js 15 + TypeScript + shadcn/ui
