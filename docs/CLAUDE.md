# Library Management System — SE2030 / IT2140
Group MLB-B11G2-10 · SLIIT · Year 2 Semester 1, 2026

## Read these before writing anything
- `docs/design-system.md` — binding. Every colour, font, spacing value,
  radius and motion duration comes from here. No exceptions.
- `docs/business-rules.md` — binding. Loan periods, fine rates, limits,
  reservation windows, moderation policy. Where a source document
  disagrees with this file, this file wins.
- `docs/` also holds the ER diagram, requirements, use-case scenarios,
  activity diagrams, sequence diagrams and both IT2140 marking documents.

## Stack
- Java 21, Spring Boot 3.x, Maven
- Thymeleaf, server-rendered. No React, no SPA, no separate frontend.
- Hand-written CSS design system. **Never add Bootstrap or Tailwind.**
- HTMX for partial page updates (live search, inline actions, modals,
  pagination). One script tag, no build step.
- Chart.js on dashboard and report screens only.
- Microsoft SQL Server 2022 via Spring Data JPA
- Spring Security for authentication and role-based access

## Non-negotiable rules
1. The schema is defined ONLY in `database/01_schema.sql`.
   `spring.jpa.hibernate.ddl-auto` stays `validate`. Never generate or
   alter tables from Java. If an entity needs a new column: edit the SQL
   file first, then the entity, then re-run the script.
2. Package by feature, never by layer:
   `com.lms.<feature>/{Service, Controller, dto}`
   Shared entities and repositories live in `com.lms.common.domain`.
   Never place one feature's class inside another feature's package.
3. **Never modify a feature package you were not asked to work on.**
   Six people share this repository.
4. Controllers return Thymeleaf view names. No `@RestController` unless
   the endpoint returns an HTMX fragment, which returns a fragment view.
5. Business rules live in services, never in controllers or templates.
   Where a stored procedure already implements a rule, call it — do not
   duplicate that logic in Java.
6. Every form-backed DTO uses Jakarta Bean Validation annotations.
7. Every UI screen uses the shared fragments in `templates/components/`.
   Write no new CSS and introduce no new colours.
8. SQL Server objects are PascalCase; Java fields are camelCase, mapped
   with `@Column(name = "...")`.
9. Configurable values (loan period, fine rate, borrowing limits,
   reservation window) are read from SYSTEM_SETTING — never hard-coded.

## Feature ownership
| Package | Function | Use cases | Owner |
|---|---|---|---|
| `com.lms.user` | User & Access Management | UC-01 | Rayyan M.R.M (IT25101420) |
| `com.lms.catalogue` | Book Catalogue & Inventory | UC-02 | Lakshitha P.M.M (IT25103166) |
| `com.lms.borrowing` | Borrowing, Returns & Renewals | UC-03 | Wijegunawardana P.K.D.S.U (IT25102574) |
| `com.lms.reservation` | Reservations & Waiting List | UC-04 | Wijewickrama S.N (IT25100467) |
| `com.lms.fine` | Fines, Appeals & Payments | UC-05, 06, 07 | Vithanage C.B (IT25102212) |
| `com.lms.report` / `.review` / `.feedback` | Reports, Reviews & Feedback | UC-08, 09, 10 | Amukotuwa A.G.S.I (IT25100041) |

Shared foundation (`com.lms.common`, security, base layout, components):
Shashith, as project lead.

## How to finish a piece of work
- Comment any decision a marker would ask "why?" about.
- Handle every alternative flow in the use-case scenario as a real case
  with a toast or inline error — never an unhandled exception page.
- When done, list every file you changed and explain in plain language
  what each one does and why.

## Why that last rule matters
SE2030 awards 15 marks for individual contribution and code
understanding, and IT2140 awards 10 marks for a viva where the student
must execute and explain SQL without assistance. Work a student cannot
explain counts as unauthorised assistance under the module's AI policy.
Every member must be able to walk through their own package line by
line. Generate code the owner can read, not code that merely runs.
