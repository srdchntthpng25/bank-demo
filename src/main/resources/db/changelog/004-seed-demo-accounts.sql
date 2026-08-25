IF NOT EXISTS (SELECT 1 FROM account WHERE account_number IN ('0000000001', '0000000002', '0000000003'))
BEGIN
    INSERT INTO account (account_number, owner_name, currency, balance, status)
    VALUES ('0000000001', 'Demo Alice', 'THB', 1000.0000, 'ACTIVE'),
           ('0000000002', 'Demo Bob', 'THB', 500.0000, 'ACTIVE'),
           ('0000000003', 'Demo Carol', 'THB', 250.0000, 'ACTIVE');

    INSERT INTO ledger_entry (account_id, transfer_id, entry_type, amount, balance_after)
    SELECT id, NULL, 'CREDIT', balance, balance
    FROM account
    WHERE account_number IN ('0000000001', '0000000002', '0000000003');
END;
