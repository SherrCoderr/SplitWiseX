-- Stage 3: Groups + Expenses.
-- Adds groups, group membership, expenses, and equal-split expense
-- participants. Does not touch the existing `users` table or any Stage 2
-- data.

CREATE TABLE groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_groups_created_by ON groups(created_by);

-- Membership join table. A user appears at most once per group (enforced
-- by the unique constraint), and the group creator is inserted here as a
-- member in the same transaction that creates the group.
CREATE TABLE group_members (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE INDEX idx_group_members_group_id ON group_members(group_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);

-- One expense belongs to exactly one group and has exactly one payer.
-- amount > 0 is enforced at both the database and application layers.
CREATE TABLE expenses (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    paid_by     BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_group_id ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by);

-- Who is splitting a given expense. Stage 3 supports equal split only, so
-- no per-participant share/percentage is persisted here — the equal share
-- is calculated on read (see ExpenseMapper) from the expense amount and
-- participant count.
CREATE TABLE expense_participants (
    id          BIGSERIAL PRIMARY KEY,
    expense_id  BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expense_participants_expense_user UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expense_participants_expense_id ON expense_participants(expense_id);
CREATE INDEX idx_expense_participants_user_id ON expense_participants(user_id);
