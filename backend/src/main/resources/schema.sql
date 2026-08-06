-- ============================================================
-- PaymentProcessing MySQL Schema
-- This file is executed by Spring Boot on startup via
-- spring.sql.init.mode=always (safe due to IF NOT EXISTS).
-- ============================================================

CREATE TABLE IF NOT EXISTS bank_accounts (
    account_id          CHAR(9)         NOT NULL,
    account_number      VARCHAR(34)     NOT NULL,
    account_holder_name VARCHAR(255)    NOT NULL,
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_bank_accounts_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beneficiaries (
    beneficiary_id  CHAR(9)         NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    account_number  VARCHAR(34)     NOT NULL,
    bank_name       VARCHAR(255)    NOT NULL,
    ifsc_code       VARCHAR(11)     NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    phone           VARCHAR(20)     NULL,
    PRIMARY KEY (beneficiary_id),
    UNIQUE KEY uk_beneficiaries_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    payment_id      CHAR(9)         NOT NULL,
    amount          DECIMAL(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    reference       VARCHAR(255)    NOT NULL,
    status          VARCHAR(50)     NOT NULL COMMENT 'CREATED | VALIDATED | SENT | COMPLETED | FAILED',
    version         BIGINT          NULL,
    payment_type    VARCHAR(50)     NOT NULL COMMENT 'BILL_PAYMENT | BENEFICIARY_TRANSFER',
    payment_method  VARCHAR(50)     NOT NULL COMMENT 'CARD | NET_BANKING | UPI',
    card_type       VARCHAR(50)     NULL COMMENT 'CREDIT_CARD | DEBIT_CARD',
    payer_id        CHAR(9)         NULL,
    invoice_id      VARCHAR(255)    NULL,
    source_account_id CHAR(9)       NULL,
    beneficiary_id  CHAR(9)         NULL,
    card_last4      VARCHAR(4)      NULL,
    card_holder_name VARCHAR(255)   NULL,
    upi_id          VARCHAR(320)    NULL,
    idempotency_key VARCHAR(255)    NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payments_idempotency_key (idempotency_key),
    UNIQUE KEY uk_payment_payer_invoice (payer_id, invoice_id),
    CONSTRAINT fk_payments_source_account
        FOREIGN KEY (source_account_id) REFERENCES bank_accounts (account_id),
    CONSTRAINT fk_payments_beneficiary
        FOREIGN KEY (beneficiary_id)    REFERENCES beneficiaries (beneficiary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE payments MODIFY source_account_id CHAR(9) NULL;
ALTER TABLE payments MODIFY beneficiary_id CHAR(9) NULL;
ALTER TABLE payments MODIFY payer_id CHAR(9) NULL;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'payment_method'
        ),
        'SELECT 1',
        "ALTER TABLE payments ADD COLUMN payment_method VARCHAR(50) NOT NULL DEFAULT 'NET_BANKING'"
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'upi_id'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN upi_id VARCHAR(320) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'card_type'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN card_type VARCHAR(50) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'card_last4'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN card_last4 VARCHAR(4) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'card_holder_name'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN card_holder_name VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS payment_history (
    history_id  CHAR(9)         NOT NULL,
    payment_id  CHAR(9)         NOT NULL,
    old_status  VARCHAR(50)     NULL     COMMENT 'NULL on first transition',
    new_status  VARCHAR(50)     NOT NULL,
    timestamp   DATETIME(6)     NOT NULL,
    remarks     VARCHAR(500)    NULL,
    error_code  VARCHAR(100)    NULL,
    actor       VARCHAR(255)    NOT NULL,
    PRIMARY KEY (history_id),
    CONSTRAINT fk_payment_history_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
