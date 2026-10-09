-- Use with psql -v admin_username=<existing-username> after that user has signed up.
-- Keep at least one active ADMIN account to prevent administrative lockout.
UPDATE public.users
SET role = 'ADMIN'
WHERE username = :'admin_username';
