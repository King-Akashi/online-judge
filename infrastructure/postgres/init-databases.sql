\getenv problem_db_password PROBLEM_DB_PASSWORD
\getenv submission_db_password SUBMISSION_DB_PASSWORD
\getenv judge_db_password JUDGE_DB_PASSWORD

SELECT format('CREATE ROLE problem_service LOGIN PASSWORD %L', :'problem_db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'problem_service') \gexec
SELECT format('CREATE ROLE submission_service LOGIN PASSWORD %L', :'submission_db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'submission_service') \gexec
SELECT format('CREATE ROLE judge_service LOGIN PASSWORD %L', :'judge_db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'judge_service') \gexec

SELECT 'CREATE DATABASE problems OWNER problem_service'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'problems') \gexec
SELECT 'CREATE DATABASE submissions OWNER submission_service'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'submissions') \gexec
SELECT 'CREATE DATABASE judge OWNER judge_service'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'judge') \gexec
