# SplitWiseX

> A full-stack real-time group expense splitter built with React, TypeScript, Spring Boot, PostgreSQL, JWT authentication, and WebSockets/STOMP.

SplitWiseX helps groups manage shared expenses, calculate who owes whom, and generate a simplified settlement plan with real-time updates across connected users.

---

## ✨ Features

### 🔐 Authentication & Security

- User registration and login
- JWT-based authentication
- BCrypt password hashing
- Stateless Spring Security configuration
- Protected REST endpoints
- Generic authentication errors to avoid account enumeration
- Server-side group membership authorization
- JWT authentication for WebSocket/STOMP connections

### 👥 Group Management

- Create expense groups
- Add members by email
- View group members
- Server-side membership validation
- Multiple users can work with the same group

### 💰 Expense Management

- Add expenses to a group
- Select the payer
- Select participating members
- Equal expense splitting
- Deterministic cent-level rounding
- Delete expenses
- Automatic recalculation after expense changes

### 📊 Balance Calculation

For every group, SplitWiseX calculates:

- How much each member should receive
- How much each member owes
- Net balance for every member

A positive balance means the member should receive money.

A negative balance means the member owes money.

### 🔄 Settlement Optimization

SplitWiseX generates a simplified settlement plan using a greedy algorithm.

Instead of showing every individual contribution, the system calculates transactions between debtors and creditors to reduce the number of payments required.

The settlement calculation uses precise monetary values to avoid floating-point money errors.

For example:

```text
Sameer   +₹243.66
Arjun    +₹512.67
Navjot   -₹756.33

The settlement plan becomes:

Navjot → Arjun   ₹512.67
Navjot → Sameer  ₹243.66

The settlement algorithm runs in:

O(n log n)

due to sorting the creditor and debtor lists.

⚡ Real-Time Updates

SplitWiseX uses WebSockets with STOMP for real-time group updates.

When a user:

Creates an expense
Deletes an expense
Adds a member

other connected members of the same group receive an event and automatically refresh their authoritative REST data.

The architecture deliberately keeps:

REST API       → source of truth
WebSocket      → real-time notification mechanism

This prevents the WebSocket layer from becoming a second source of truth for application state.

🎨 Modern React UI
Responsive React interface
TypeScript
Tailwind CSS
Premium SaaS-style design
Dashboard summary cards
Group cards
Expense views
Balance and settlement views
Loading states
Empty states
Error states
Form validation
Custom confirmation dialogs
Escape-to-close modal behavior
Responsive layouts
WebSocket connection status indicator
🛠️ Tech Stack
Frontend
React 18
TypeScript
Vite
Tailwind CSS
Axios
STOMP.js
WebSockets
Backend
Java 17
Spring Boot 3.3
Spring Security
Spring Data JPA
Hibernate
Maven
JWT
BCrypt
WebSocket / STOMP
Database
PostgreSQL 16
Flyway migrations
Development
Docker
Docker Compose
Git / GitHub
🏗️ Architecture
                    ┌─────────────────────┐
                    │      React UI       │
                    │ React + TypeScript  │
                    └──────────┬──────────┘
                               │
                    REST API   │   WebSocket/STOMP
                               │
              ┌────────────────┴────────────────┐
              │                                 │
              ▼                                 ▼
    ┌─────────────────────┐          ┌─────────────────────┐
    │   Spring Boot API   │          │ WebSocket Broker    │
    │                     │          │                     │
    │ Controllers         │          │ Group subscriptions │
    │ Services            │          │ Real-time events    │
    │ Security            │          └─────────────────────┘
    │ Repositories        │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │     PostgreSQL       │
    │                     │
    │ Users               │
    │ Groups              │
    │ Members             │
    │ Expenses            │
    │ Participants        │
    └─────────────────────┘
🔄 Real-Time Architecture

SplitWiseX does not use WebSockets as the primary source of application state.

The flow is:

User A
   │
   │ REST request
   ▼
Spring Boot
   │
   │ Database transaction
   ▼
PostgreSQL
   │
   │ Transaction committed
   ▼
Group Change Event
   │
   ▼
WebSocket/STOMP
   │
   ▼
Other connected users
   │
   │ Receive notification
   ▼
Frontend refetches REST data
   │
   ▼
Updated UI

This provides a clean separation:

REST handles state changes and authoritative data.
PostgreSQL stores the actual state.
WebSockets notify connected clients that something changed.
Clients refetch the latest state through REST.

WebSocket events currently include:

EXPENSE_CREATED
EXPENSE_DELETED
MEMBER_ADDED
💸 Expense Splitting

For equal splitting, SplitWiseX divides an expense between the selected participants.

For example:

Expense = ₹100
Participants = 3

The system distributes the amount deterministically:

Member A → ₹33.34
Member B → ₹33.33
Member C → ₹33.33

The total remains exactly:

₹100.00

This avoids rounding inconsistencies when an expense cannot be divided evenly.

🧮 Settlement Algorithm

After calculating each member's net balance, SplitWiseX separates users into:

Creditors
    ↓
Members who should receive money

Debtors
    ↓
Members who owe money

A greedy approach then matches debtors with creditors.

For example:

Sameer   +₹243.66
Arjun    +₹512.67
Navjot   -₹756.33

The settlement plan becomes:

Navjot → Arjun   ₹512.67
Navjot → Sameer  ₹243.66

Instead of requiring multiple unnecessary transfers, the algorithm directly matches outstanding balances.

The implementation uses precise monetary values and runs in:

O(n log n)

due to sorting the creditor and debtor lists.

🗄️ Database

The application uses PostgreSQL with Flyway-managed migrations.

Current migrations:

V1__create_users_table.sql
V2__create_groups_and_expenses.sql

The core relationships are:

User
 │
 └── GroupMember
          │
          ▼
        Group
          │
          ▼
       Expense
          │
          ▼
  ExpenseParticipant

Hibernate is configured with:

ddl-auto = validate

This means Hibernate validates the existing schema instead of silently modifying it.

Flyway is responsible for version-controlled schema migrations.

📁 Project Structure
splitwisex/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/splitwisex/
│       │   │
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── event/
│       │   ├── exception/
│       │   ├── mapper/
│       │   ├── repository/
│       │   ├── security/
│       │   ├── service/
│       │   └── websocket/
│       │
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               ├── V1__create_users_table.sql
│               └── V2__create_groups_and_expenses.sql
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/
│       ├── components/
│       ├── context/
│       ├── lib/
│       ├── pages/
│       └── types/
│
├── docker-compose.yml
├── .gitignore
└── README.md
🚀 Getting Started
Prerequisites

Make sure you have:

Java 17+
Maven 3.9+
Node.js 18+
npm
Docker Desktop
Git
🐘 1. Start PostgreSQL

From the project root:

docker compose up -d

Check the container:

docker compose ps

The SplitWiseX PostgreSQL container runs on:

localhost:5433

The default development database configuration is:

Database: splitwisex
User:     splitwisex
Password: splitwisex
Port:     5433
☕ 2. Start the Backend

Open a terminal:

cd backend

Run:

mvn spring-boot:run

The backend starts on:

http://localhost:8080

Flyway automatically validates and applies pending migrations during startup.

⚛️ 3. Start the Frontend

Open another terminal:

cd frontend

Install dependencies:

npm install

Start the development server:

npm run dev

Open:

http://localhost:5173
❤️ Health Check

The backend exposes:

GET /api/health

Open:

http://localhost:8080/api/health

Expected response:

{
  "service": "splitwisex-backend",
  "status": "UP"
}
🔐 Environment Variables

The backend supports the following environment variables:

Variable	Default	Purpose
DB_NAME	splitwisex	PostgreSQL database
DB_USER	splitwisex	PostgreSQL username
DB_PASSWORD	splitwisex	PostgreSQL password
JWT_SECRET	Development placeholder	JWT signing secret
JWT_EXPIRATION_MS	86400000	JWT lifetime

The development defaults are provided for local use.

For production deployment, replace the development JWT secret with a strong secret and configure production database credentials through environment variables.

🧪 Testing & Verification

The backend contains unit tests covering:

Authentication
JWT functionality
Expenses
Groups
Balances
Settlement calculations

Run:

cd backend
mvn clean test

The verified Stage 7 project currently passes:

Tests run: 45
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

The frontend production build was also verified successfully:

cd frontend
npm run build

The build completed successfully with TypeScript compilation and Vite production bundling.

The application was also manually verified for:

Login and logout
Dashboard
Group management
Expense creation
Expense deletion
Balance recalculation
Settlement recalculation
Custom delete confirmation
Escape-to-close modal behavior
WebSocket connection status
Real-time expense creation
Real-time expense deletion
Multi-user group synchronization
🔌 API Overview
Authentication
POST /api/auth/register
POST /api/auth/login
Health
GET /api/health
Groups

Group-related REST endpoints support:

Creating groups
Retrieving groups
Adding members
Retrieving group members
Expenses

Expense endpoints support:

Creating expenses
Retrieving expenses
Deleting expenses
Balances

Balance endpoints provide:

Member balances
Group balances
Settlement transactions

The backend remains the authoritative source for these calculations.

🔒 Security Model

SplitWiseX uses JWT-based authentication.

The general request flow is:

Login/Register
      │
      ▼
JWT issued
      │
      ▼
Frontend stores token
      │
      ▼
Axios adds:
Authorization: Bearer <token>
      │
      ▼
JwtAuthenticationFilter
      │
      ▼
User loaded from PostgreSQL
      │
      ▼
Spring SecurityContext
      │
      ▼
Protected endpoint

Group-specific operations also perform server-side membership checks.

This prevents users from accessing or modifying groups they do not belong to.

🖥️ Frontend

The frontend uses:

React
TypeScript
React Context for authentication state
Axios for REST communication
STOMP.js for WebSocket communication
Tailwind CSS for styling
Vite for development and production builds

Important frontend areas include:

src/
├── api/
├── components/
├── context/
├── lib/
├── pages/
└── types/

The application includes dedicated pages for:

Landing
Login
Registration
Dashboard
Group Details
Add Expense
Profile
📱 Responsive UI

The application was manually verified across different viewport sizes, including:

Mobile
Tablet
Desktop

The interface includes:

Responsive layouts
Loading states
Empty states
Validation feedback
Custom confirmation dialogs
WebSocket connection status
📸 Screenshots

Screenshots can be added here to showcase the application.

Recommended screenshots:

Landing page
Dashboard
Group details
Expense creation
Balances and settlements
Real-time WebSocket update
Profile page
🗺️ Development Roadmap

The project was developed incrementally:

 Stage 1 — Project foundation
 Stage 2 — Authentication, JWT, Spring Security
 Stage 3 — Groups, members, expenses, PostgreSQL relationships
 Stage 4 — Balance calculation and settlement algorithm
 Stage 5 — Dashboard and UI/UX implementation
 Stage 6 — WebSockets and real-time group updates
 Stage 7 — Final UI polish, accessibility, validation and UX refinement
Potential Future Improvements

Possible future additions include:

Production deployment
Automated integration / end-to-end tests
Expense categories
Unequal/custom expense splits
Notifications
Advanced analytics
Group activity history
More sophisticated settlement optimization
Production-grade observability
Refresh-token based authentication
Cloud database deployment
🎯 What This Project Demonstrates

SplitWiseX demonstrates practical full-stack engineering across several areas.

Frontend
React architecture
TypeScript
REST API integration
State management
Responsive UI
Form validation
Real-time UI updates
Backend
Spring Boot
REST API design
Spring Security
JWT authentication
Service/repository architecture
DTOs and mappers
Exception handling
WebSockets/STOMP
Database
PostgreSQL
Relational modeling
JPA/Hibernate
Flyway migrations
Transactional data handling
Algorithms
Balance calculation
Greedy settlement optimization
Precise monetary calculations
O(n log n) settlement processing
👨‍💻 Author

Sameer

Computer Science Engineering student building full-stack applications with Java, Spring Boot, React, PostgreSQL, and modern web technologies.

GitHub:

https://github.com/SherrCoderr

📄 License

This project is currently maintained as a personal portfolio project.


**Important:** Copy from the first `# SplitWiseX` all the way to the final sentence. Don't copy the ```markdown markers themselves.

After replacing the file, **save it**. Then