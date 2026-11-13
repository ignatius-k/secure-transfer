# Secure Transfer API

A Spring Boot REST API for transferring money between accounts held within the same financial institution.

**Stack:** Java 17+, Spring Boot 3.3, Spring Security (OAuth2 resource server, RS256 JWT), Spring Data JPA, PostgreSQL 16, Flyway, JUnit 5 + Mockito.

---

## 1. Setup

**Prerequisites:** JDK 17+, Maven 3.8+, Docker.

### Environment

```bash
cp .env.example .env   # then adjust values if needed
```

| Variable | Default | Purpose |
|---|---|---|
| `DB_NAME` | `secure_transfer` | Database name |
| `DB_USERNAME` / `DB_PASSWORD` | `secure_transfer` | Database credentials |
| `DB_PORT` | `5433` | Host port for PostgreSQL |
| `APP_PORT` | `8090` | Host port for the API |
| `DEMO_DATA_ENABLED` | `true` | Seed demo users and accounts on first start |

### Option A: Docker Compose (app + PostgreSQL)

```bash
mvn -DskipTests package       # build the jar
docker compose up -d --build  # start PostgreSQL and the API
```
API: **http://localhost:8090**. Stop: `docker compose down` (add `-v` to wipe data).

### Option B: Run locally with Maven

```bash
docker compose up -d postgres   # PostgreSQL only
mvn spring-boot:run             # API on http://localhost:8080
```

### Tests

```bash
mvn test
```

### Demo data (seeded on first start)

All demo users have the password **`Password@123`**.

| User | Role | Account | Balance | Status |
|---|---|---|---|---|
| `jane.doe` | CUSTOMER | 1000000001 | 50,000.00 | ACTIVE |
| `john.smith` | CUSTOMER | 1000000002 | 10,000.00 | ACTIVE |
| `peter.okello` | CUSTOMER | 1000000003 | 5,000.00 | INACTIVE |
| `ops.mary` | OPERATIONS (all staff permissions) | – | – | – |
| `recon.paul` | RECON_OFFICER (TRANSFER_RECONCILE, TRANSFER_READ only) | – | – | – |
| – | internal | 9000000001 Reconciliation Suspense | 1,000,000.00 | ACTIVE |

---

## 2. Assumptions

1. All accounts are held within a single institution, in a **single currency**, so currency is not modelled.
2. **One account per customer** (`accounts.customer_id` is unique). A customer's source account is resolved from their token, so the customer request has no source field.
3. The client generates **`transactionRef`** (a UUID). It is both the transaction's unique reference and the duplicate-transfer guard.
4. `initiatedBy` always comes from the authenticated token, never from the request body.
5. Transfers are synchronous: a transfer is either `SUCCESS` or `FAILED` (no `PENDING`).
6. Authentication uses username/password to obtain a JWT. A production deployment would move token issuance to a dedicated authorization server (Keycloak or Spring Authorization Server) without changing the transfer code.

---

## 3. API

Full specification: [`docs/openapi.yaml`](docs/openapi.yaml) (OpenAPI 3.0; open it in https://editor.swagger.io).

| Method & route | Access | Purpose |
|---|---|---|
| `POST /api/v1/auth/token` | public | Exchange credentials for a JWT |
| `POST /api/v1/transfers` | role `CUSTOMER` | Customer transfer from own account |
| `POST /api/v1/operations/transfers` | permission per type (see below) | Staff transfer between any two accounts |
| `GET /api/v1/transfers/{transactionRef}` | `CUSTOMER` (own only) or `TRANSFER_READ` | View a transfer |
| `GET /api/v1/accounts/{accountNumber}` | `CUSTOMER` (own only) or `ACCOUNT_READ` | View an account |

**Customers are checked by role and staff by permission.** On the operations endpoint, each transfer type needs its own permission:

| Type | Permission |
|---|---|
| `RECONCILIATION` | `TRANSFER_RECONCILE` |
| `AGENT_TRANSFER` | `TRANSFER_AGENT` |
| `NORMAL_TRANSFER` | not allowed (customer endpoint only) → 400 |

### Sample requests

```bash
BASE=http://localhost:8090/api/v1   # Docker Compose; use 8080 when running with mvn spring-boot:run

# Get a token
TOKEN=$(curl -s -X POST $BASE/auth/token -H 'Content-Type: application/json' \
  -d '{"username":"jane.doe","password":"Password@123"}' | jq -r .access_token)
```
```json
{ "access_token": "eyJhbGciOiJSUzI1NiJ9...", "token_type": "Bearer", "expires_in": 900 }
```

**Customer transfer**
```bash
curl -i -X POST $BASE/transfers -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "transactionRef": "8f14e45f-ceea-4e7a-9a6b-2c3d4e5f6a7b",
  "destinationAccountNumber": "1000000002",
  "amount": "500.00",
  "narration": "Rent"
}'
```
`201 Created`, `Location: /api/v1/transfers/8f14e45f-...`
```json
{
  "transactionRef": "8f14e45f-ceea-4e7a-9a6b-2c3d4e5f6a7b",
  "status": "SUCCESS",
  "type": "NORMAL_TRANSFER",
  "direction": "DEBIT",
  "sourceAccountNumber": "1000000001",
  "destinationAccountNumber": "1000000002",
  "destinationAccountName": "John S****",
  "amount": "500.00",
  "narration": "Rent",
  "createdAt": "2026-11-13T08:14:06.290345Z"
}
```
Sending the same request again returns `200 OK` with the original body and the header `Idempotent-Replayed: true`. No money moves a second time.

**Operations (staff) transfer**
```bash
STAFF=$(curl -s -X POST $BASE/auth/token -H 'Content-Type: application/json' \
  -d '{"username":"ops.mary","password":"Password@123"}' | jq -r .access_token)

curl -i -X POST $BASE/operations/transfers -H "Authorization: Bearer $STAFF" -H 'Content-Type: application/json' -d '{
  "transactionRef": "3b1d7c2a-9e4f-4a8b-b6c5-1d2e3f4a5b6c",
  "type": "RECONCILIATION",
  "sourceAccountNumber": "9000000001",
  "destinationAccountNumber": "1000000002",
  "amount": "1000.00",
  "reason": "Recon RC-55: misposted branch deposit",
  "metadata": { "reconItemId": "RC-55" }
}'
```
`201 Created`
```json
{
  "transactionRef": "3b1d7c2a-9e4f-4a8b-b6c5-1d2e3f4a5b6c",
  "status": "SUCCESS",
  "type": "RECONCILIATION",
  "sourceAccountNumber": "9000000001",
  "destinationAccountNumber": "1000000002",
  "destinationAccountName": "John Smith",
  "amount": "1000.00",
  "reason": "Recon RC-55: misposted branch deposit",
  "initiatedBy": "ops.mary",
  "metadata": { "reconItemId": "RC-55" },
  "createdAt": "2026-11-13T08:14:07.345535Z"
}
```

**Reads**
```bash
curl $BASE/transfers/8f14e45f-ceea-4e7a-9a6b-2c3d4e5f6a7b -H "Authorization: Bearer $TOKEN"
curl $BASE/accounts/1000000001 -H "Authorization: Bearer $TOKEN"
```
```json
{ "accountNumber": "1000000001", "accountName": "Jane Doe", "status": "ACTIVE", "balance": "49500.00", "asOf": "..." }
```
A customer requesting someone else's transfer or account gets **404**, not 403, so they cannot probe which refs or accounts exist. Staff views also include `reason`, `initiatedBy` and `metadata`. Customers never see these.

### Errors

Every error uses the same shape (RFC 9457 fields), with `Content-Type: application/problem+json`:
```json
{
  "title": "Insufficient funds",
  "status": 422,
  "detail": "Source account balance is insufficient for this transfer.",
  "instance": "/api/v1/transfers",
  "errorCode": "INSUFFICIENT_FUNDS",
  "transactionRef": "736aa6d4-52c2-4c56-a724-abe01b43fd82",
  "traceId": "fd61ed24e6c542ca",
  "timestamp": "2026-11-13T08:14:06.666Z"
}
```

| Status | errorCode | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` (with `errors[]`), `MALFORMED_REQUEST`, `SAME_ACCOUNT_TRANSFER`, `INVALID_AMOUNT`, `TRANSFER_TYPE_NOT_ALLOWED`, `INVALID_METADATA` | Bad request (not recorded) |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` | Missing, invalid or expired token; wrong login |
| 403 | `FORBIDDEN` | Missing role or permission |
| 404 | `ACCOUNT_NOT_FOUND`, `TRANSFER_NOT_FOUND` | Not found, or not yours |
| 409 | `DUPLICATE_TRANSACTION_REF` | Same `transactionRef`, different payload |
| 422 | `INSUFFICIENT_FUNDS`, `ACCOUNT_INACTIVE` | Business rejection (recorded as FAILED) |
| 503 | `SERVICE_BUSY` | Row lock could not be acquired; retry with the same ref |
| 500 | `INTERNAL_ERROR` | Unexpected; details only in logs, linked by `traceId` |

Every response carries an `X-Trace-Id` header, which also appears in the log lines.

---

## 4. Design

### One transfer engine
```
CustomerTransferController   ─► CustomerTransferService   ─┐
OperationsTransferController ─► OperationsTransferService ─┴─► TransferEngine.execute(TransferCommand)
```
`TransferEngine` is the **only** code that moves money. Callers build a `TransferCommand` and handle the result. They never write to `accounts` or `transactions`. The engine **does not trust callers**: it re-checks every rule even if the caller already did. New flows such as reconciliation or agent banking become new callers, not new posting code.

| Engine (every transfer) | Callers (context-specific) |
|---|---|
| Amount > 0, at most 2 decimal places; source ≠ destination | Customer: source = own account; type = `NORMAL_TRANSFER` |
| Both accounts exist and are ACTIVE; sufficient funds | Operations: permission for the requested type; mandatory reason; metadata limits |
| Duplicate protection; row locking; atomic debit + credit + record | |

### Transactions and rollback
- `execute()` is `@Transactional(propagation = REQUIRED, rollbackFor = Exception.class)`. The debit, credit and transaction record commit together, and any exception rolls back all three, so there are **no partial updates**.
- The engine is its **own bean**. `@Transactional` only applies through Spring's proxy, so a call from inside the same class would silently run without a transaction.
- With `REQUIRED`, a caller that is already transactional (e.g. a future reconciliation service that also marks a recon item RESOLVED) has the transfer **join** its transaction. Both commit or roll back together.
- Business rules are checked **before** any balance changes. A rejection therefore has nothing to undo: the engine saves a `FAILED` record (with `failure_reason`) and returns. Invalid requests (400) are not recorded, and technical errors roll everything back.
- Database safety nets: `CHECK (balance >= 0)` and `CHECK (amount > 0)`.

### Concurrency
- Both accounts are loaded with `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`), **always in sorted account-number order**. Concurrent A→B and B→A transfers therefore cannot deadlock.
- Verified manually: 10 parallel transfers of 2,000 from an 11,500 balance gave exactly 5 × 201 and 5 × 422, and the balance never went negative.

### Duplicate transfers (`transactionRef`)
- The `UNIQUE (transaction_ref)` constraint is the final guard.
- `request_hash` (SHA-256 of the business payload) tells a safe replay apart from a conflicting reuse:
  - **Same ref, same payload:** the stored outcome is returned (`200` + `Idempotent-Replayed: true`, or the same FAILED error).
  - **Same ref, different payload:** `409`.
  - A FAILED ref stays failed. To retry, the client sends a new ref.
- The ref is checked **after** the account locks are taken. Identical concurrent requests lock the same rows, so the second waits and then sees the first one's committed record. Verified manually: 10 identical parallel requests gave 1 × 201 + 9 × 200 replays and one row.

### Security
- `POST /auth/token` checks the BCrypt password and issues a 15-minute **RS256** JWT with `sub`, `preferred_username`, `roles` and `permissions`.
  - Unknown users still go through a BCrypt comparison, so response timing doesn't reveal which usernames exist.
- The resource server validates the signature, expiry and issuer. `roles` become `ROLE_*` authorities and `permissions` become plain authorities.
- Endpoints are guarded with `@PreAuthorize`, plus a URL rule on `/api/v1/operations/**` as a second layer.
- **Ownership:** customers can only debit, view or read their own account and transfers.

### Database
- **Flyway owns the schema** (`V1` security tables, `V2` accounts, `V3` transactions). Hibernate runs with `ddl-auto=validate`, so it never changes the schema.
- **Permissions are synced from code at startup** (`PermissionSync`). The `PermissionCode` enum is the catalogue.
  - Missing permissions are inserted. Permissions removed from code are marked `deprecated`, never deleted.
  - The `CUSTOMER` and `OPERATIONS` roles are created if missing.
  - The sync is idempotent and safe across restarts.
- `transactions.metadata` (`JSONB`) holds extra caller context, such as a recon item id or agent code.
  - The engine stores it but never acts on it.
  - It must be a flat object, at most 4 KB, and cannot use keys that shadow core fields.
  - Customers cannot send it: the server sets `channel`. Only staff can see it.
- Account numbers in `transactions` have no foreign key, so a FAILED record can still be kept for an account that doesn't exist.

---

## 5. Tests

`TransferEngineTest` (JUnit 5 + Mockito) covers the core rules:

| Test | Verifies |
|---|---|
| `transfer_success_debitsAndCredits` | Balances move; SUCCESS record with the correct details |
| `transfer_insufficientFunds_failsWithoutChangingBalances` | FAILED / `INSUFFICIENT_FUNDS`; balances untouched |
| `transfer_inactiveAccount_fails` | FAILED / `ACCOUNT_INACTIVE`; balances untouched |
| `transfer_sameAccount_rejected` | 400; no locks taken, nothing recorded |

Run with `mvn test`.

**Next step:** integration tests with Testcontainers PostgreSQL covering rollback mid-transfer, duplicate refs, parallel transfers, and 401/403/ownership. These need a real database for `FOR UPDATE`, the unique constraint and `jsonb`.

---

## 6. Future improvements

- Multiple accounts per customer, with a default account and an optional `sourceAccountNumber`.
- Maker-checker approval for operations transfers.
- Refresh tokens, token revocation, login rate limiting and lockout.
- Keys loaded from a vault/keystore, plus a JWKS endpoint; or an external authorization server.
- Transaction limits (per transfer / daily).
- A `REQUIRES_NEW` audit write, so FAILED records survive even when a joining caller rolls back.
- Double-entry ledger tables, and an outbox for publishing transfer events.
