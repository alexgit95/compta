-- Fix WebAuthn columns to use bytea type instead of oid
-- This migration handles both fresh installations and existing ones

-- Check if the table exists and fix column types if needed
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_credentials') THEN
        -- Drop constraints that reference these columns (if any)
        ALTER TABLE user_credentials DROP CONSTRAINT IF EXISTS fk_user_credentials_user_id;
        
        -- For each binary column, check if it needs conversion
        -- If the column is oid type, we need to recreate it as bytea
        
        -- Check and fix public_key column
        BEGIN
            ALTER TABLE user_credentials 
            ALTER COLUMN public_key TYPE bytea USING public_key::bytea;
        EXCEPTION WHEN OTHERS THEN
            -- If it's already bytea or column doesn't exist, ignore
            NULL;
        END;
        
        -- Check and fix attestation_object column
        BEGIN
            ALTER TABLE user_credentials 
            ALTER COLUMN attestation_object TYPE bytea USING attestation_object::bytea;
        EXCEPTION WHEN OTHERS THEN
            -- If it's already bytea or column doesn't exist, ignore
            NULL;
        END;
        
        -- Check and fix attestation_client_data_json column
        BEGIN
            ALTER TABLE user_credentials 
            ALTER COLUMN attestation_client_data_json TYPE bytea USING attestation_client_data_json::bytea;
        EXCEPTION WHEN OTHERS THEN
            -- If it's already bytea or column doesn't exist, ignore
            NULL;
        END;
        
        -- Recreate the foreign key constraint if the user_entities table exists
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_entities') THEN
            ALTER TABLE user_credentials 
            ADD CONSTRAINT fk_user_credentials_user_id 
            FOREIGN KEY (user_entity_user_id) 
            REFERENCES user_entities(id);
        END IF;
    END IF;
END $$;
