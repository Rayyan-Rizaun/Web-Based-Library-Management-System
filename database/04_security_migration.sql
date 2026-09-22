/* =============================================================================
   Library Management System — UC-01 security migration
   IT2140 Part 02 · Group MLB-B11G2-10 · Microsoft SQL Server 2022
   -----------------------------------------------------------------------------
   New objects only: the PasswordResetToken table and the four Security.*
   SystemSetting rows added to database/01_schema.sql this session. Safe to
   run once against the existing LibraryDB without re-running 01_schema.sql.
   ============================================================================= */

SET NOCOUNT ON;
GO

IF OBJECT_ID(N'dbo.PasswordResetToken', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PasswordResetToken (
        TokenID     INT IDENTITY(1,1) NOT NULL,
        UserID      INT               NOT NULL,
        TokenHash   NVARCHAR(64)      NOT NULL,   -- SHA-256, hex-encoded
        ExpiresAt   DATETIME2(0)      NOT NULL,
        ConsumedAt  DATETIME2(0)      NULL,       -- NULL until the token is used to set a new password
        CreatedAt   DATETIME2(0)      NOT NULL CONSTRAINT DF_PasswordResetToken_CreatedAt DEFAULT (SYSDATETIME()),
        RequestIP   NVARCHAR(45)      NULL,

        CONSTRAINT PK_PasswordResetToken PRIMARY KEY (TokenID),
        CONSTRAINT UQ_PasswordResetToken_TokenHash UNIQUE (TokenHash),
        CONSTRAINT FK_PasswordResetToken_AppUser FOREIGN KEY (UserID)
            REFERENCES dbo.AppUser (UserID) ON DELETE CASCADE ON UPDATE NO ACTION,
        CONSTRAINT CK_PasswordResetToken_TokenHashFormat CHECK (LEN(TokenHash) = 64 AND TokenHash NOT LIKE N'%[^0-9a-f]%'),
        CONSTRAINT CK_PasswordResetToken_ExpiresAfterCreated CHECK (ExpiresAt > CreatedAt),
        CONSTRAINT CK_PasswordResetToken_ConsumedAfterCreated CHECK (ConsumedAt IS NULL OR ConsumedAt >= CreatedAt)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'IX_PasswordResetToken_Lookup'
      AND object_id = OBJECT_ID(N'dbo.PasswordResetToken')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_PasswordResetToken_Lookup
        ON dbo.PasswordResetToken (TokenHash) WHERE ConsumedAt IS NULL;
END
GO

INSERT INTO dbo.SystemSetting (SettingKey, SettingValue, SettingDataType, Description)
SELECT v.SettingKey, v.SettingValue, v.SettingDataType, v.Description
FROM (VALUES
    (N'Security.LockoutMaxAttempts',        N'5',  N'Int', N'Failed logins allowed for one email before the account is locked (business-rules §8).'),
    (N'Security.LockoutWindowMinutes',      N'15', N'Int', N'Rolling window the failed-login count above is measured over (business-rules §8).'),
    (N'Security.SessionTimeoutMinutes',     N'30', N'Int', N'Idle minutes before a session expires and the next request must log in again (business-rules §8).'),
    (N'Security.PasswordResetTokenMinutes', N'30', N'Int', N'Minutes a password-reset link stays valid after it is requested (business-rules §8).')
) AS v(SettingKey, SettingValue, SettingDataType, Description)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.SystemSetting s WHERE s.SettingKey = v.SettingKey
);
GO

