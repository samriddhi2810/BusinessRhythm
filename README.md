# Business rhythm

Business rhythm is a full-stack order and inventory manager based on the original StockFlow project proposal: React frontend, Java 21 / Spring Boot REST API, and MySQL storage.

**New to the project?** Follow [Run Business rhythm in VS Code](RUN_IN_VSCODE.md) for the complete Windows setup. Local login: **admin** / **change-me-local**.

The requested [requirements.txt](requirements.txt) documents that there are no Python dependencies. Install frontend packages with npm and backend packages with Maven; a Python virtual environment is optional.

## Quick start with Docker

Install Docker Desktop with Compose, open a terminal in this directory, then run:

```sh
docker compose up --build
```

Open http://localhost:5173 and sign in with **admin** / **change-me-local**. The first build downloads dependencies and runs backend tests. MySQL data persists in the `mysql_data` volume. Stop using `docker compose down`.

Copy `.env.example` to `.env` to set your own credentials before the first run. Local defaults are for development. Use HTTPS and unique secrets before exposing this application publicly. The ports are bound to localhost.

## Run without Docker

Requires Java 21, Maven 3.9+, Node 22.12+ (or Node 24), and MySQL 8.0.16+ or 8.4. The local demo was verified with MySQL 8.0.40.

Create the database and user as a MySQL administrator:

```sql
CREATE DATABASE stockflow;
CREATE USER 'stockflow'@'localhost' IDENTIFIED BY 'stockflow_local';
GRANT ALL PRIVILEGES ON stockflow.* TO 'stockflow'@'localhost';
```

In `backend`, run `mvn spring-boot:run`. In a second terminal, in `frontend`, run `npm ci` then `npm run dev`. Open http://localhost:5173. Vite proxies `/api` to port 8080.

Backend environment overrides: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_USER`, `APP_PASSWORD`. The schema is created automatically. Login uses Spring Security, BCrypt, and stateless HTTP Basic authentication. Credentials remain in frontend memory until sign-out or reload. This single-owner MVP configures the owner through environment variables rather than a user administration table.

## Included workflows

- Dashboard with active product count, open orders, low stock and completed sales.
- Product creation, editing, search and archiving; inventory quantities and minimum levels.
- Customer order creation with multiple unique products and historical unit prices.
- Order details and history, status filtering, and controlled status changes.
- Low-stock alerts when stock is at or below the threshold.
- Completed sales totals and daily summaries in INR (change the frontend currency formatter if required).

Orders follow `Pending → Confirmed → Packed → Completed`. Pending, Confirmed and Packed orders can be cancelled. Confirmation checks live stock and deducts it atomically. Cancelling a Confirmed or Packed order restores stock once. Pending cancellation does not affect stock. Completed and Cancelled orders are final. Failed confirmation leaves the order Pending; cancel it and create a corrected order if quantities need to change.

Product and order rows are locked during stock-sensitive transactions. Product locks are acquired in ID order. All items confirm or roll back together. Orders store unit prices when created, so product price edits do not change history. Archiving hides a product from the catalog, retains historical references and blocks new confirmation of pending orders containing it. Existing confirmed orders can still complete or cancel. Inventory edits set the current physical count, rather than adding a quantity.

Reports count Completed orders only. Daily sales use order creation dates; they show the latest 30 dates with completed sales, while the headline is all time. Customers are stored with each order's entered details; customer administration is outside this MVP. No sample business data is inserted.

## API

All routes require an HTTP Basic Authorization header.

| Method | Route | Purpose |
|---|---|---|
| GET | `/api/me` | Verify login |
| GET / POST | `/api/products` | List / create products |
| PUT / DELETE | `/api/products/{id}` | Edit / archive product |
| GET / POST | `/api/orders` | List / create orders |
| GET | `/api/orders/{id}` | Order and item details |
| PATCH | `/api/orders/{id}/status` | Change status |
| GET | `/api/reports` | Dashboard and sales summaries |

Product body: `{"name":"Tea","price":12.50,"stock":20,"minimumStock":5}`.

Order body: `{"customerName":"Alex","contact":"555-0100","items":[{"productId":1,"quantity":2}]}`.

Status body: `{"status":"Confirmed"}`.

## Verification

Run `mvn test` in `backend` and `npm run build` in `frontend`. Backend integration tests exercise stock deduction, restoration, invalid transitions, insufficient stock, price snapshots, reports and archival behavior using H2 in MySQL mode. A real MySQL concurrency/load test is still appropriate before production deployment.

Manual demo: add a product with stock 5 and minimum 2; create an order for 3; confirm it and observe stock 2 and a low-stock alert; cancel it and observe stock 5. Create another order, confirm, pack and complete it, then inspect Reports. Try confirming an order exceeding current stock; it must remain Pending.

## Layout

`backend/src/main/java/com/stockflow`: REST controller, transaction service, security and application entry point. `backend/src/main/resources`: database schema and settings. `backend/src/test`: business-rule integration tests. `frontend/src`: React screens and responsive styling. `compose.yaml`: local three-service setup.

Supplier orders, barcode scanning, multiple roles, exportable reports and notifications remain outside the proposal's MVP.
