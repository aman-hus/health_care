-- Run once against the hospital_management database if users.role is a MySQL ENUM.
-- JPA stores Role as its enum name (EnumType.STRING), so VARCHAR supports new roles
-- without requiring a database schema change for every added Role constant.
ALTER TABLE users
    MODIFY COLUMN role VARCHAR(32) NOT NULL;
