ALTER TABLE account DROP CONSTRAINT DF_account_created;
ALTER TABLE account DROP CONSTRAINT DF_account_updated;
ALTER TABLE transfer DROP CONSTRAINT DF_transfer_created;
ALTER TABLE ledger_entry DROP CONSTRAINT DF_ledger_created;
ALTER TABLE outbox_event DROP CONSTRAINT DF_outbox_created;
DROP INDEX IX_ledger_account ON ledger_entry;

ALTER TABLE account ALTER COLUMN created_at datetimeoffset(6) NOT NULL;
ALTER TABLE account ALTER COLUMN updated_at datetimeoffset(6) NOT NULL;
ALTER TABLE transfer ALTER COLUMN created_at datetimeoffset(6) NOT NULL;
ALTER TABLE ledger_entry ALTER COLUMN created_at datetimeoffset(6) NOT NULL;
ALTER TABLE outbox_event ALTER COLUMN created_at datetimeoffset(6) NOT NULL;
ALTER TABLE outbox_event ALTER COLUMN published_at datetimeoffset(6) NULL;

ALTER TABLE account ADD CONSTRAINT DF_account_created DEFAULT (SYSUTCDATETIME()) FOR created_at;
ALTER TABLE account ADD CONSTRAINT DF_account_updated DEFAULT (SYSUTCDATETIME()) FOR updated_at;
ALTER TABLE transfer ADD CONSTRAINT DF_transfer_created DEFAULT (SYSUTCDATETIME()) FOR created_at;
ALTER TABLE ledger_entry ADD CONSTRAINT DF_ledger_created DEFAULT (SYSUTCDATETIME()) FOR created_at;
ALTER TABLE outbox_event ADD CONSTRAINT DF_outbox_created DEFAULT (SYSUTCDATETIME()) FOR created_at;
CREATE INDEX IX_ledger_account ON ledger_entry (account_id, created_at DESC);