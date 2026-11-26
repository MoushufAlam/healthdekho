-- =====================================================================
-- DEMO DATA: a policy with OVERDUE installments, to show the 10% late penalty.
--
-- The README sample data has no overdue installment (all due dates are in the future),
-- so this script adds one demo policy for Rohan Verma (customer 1003):
--   policy  HDP-DEMO-0001  (Smart Savings, quarterly 7500.00, started 2026-05-20)
--   installment 9001  due 2026-05-20, grace ends 2026-06-20  -> OVERDUE
--   installment 9002  due 2026-08-20, grace ends 2026-09-20  -> OVERDUE
--   installment 9003  due 2026-11-20, grace ends 2026-12-20  -> pending (overdue after 20 Dec 2026)
--
-- Paying 9001 late: outstanding 7500.00 + 10% penalty 750.00 = 8250.00 must be paid in full.
--
-- NOT a Flyway migration: run it by hand in pgAdmin (database "healthdekho") while the app is running.
-- Run it once. To undo, use reset-dev-db.sql and restart the app.
-- =====================================================================

INSERT INTO customer_policy (policy_id, policy_number, customer_id, product_id, start_date, maturity_date,
                             premium_frequency, premium_amount, sum_insured, total_premium_payable, status)
VALUES (9001, 'HDP-DEMO-0001', 1003, 203, '2026-05-20', '2036-05-19',
        'QUARTERLY', 7500.00, 1500000.00, 300000.00, 'ACTIVE');

INSERT INTO premium_installment (installment_id, policy_id, installment_number, due_date, grace_period_end_date,
                                 scheduled_amount, paid_amount, payment_status)
SELECT 9000 + n,
       9001,
       n,
       (DATE '2026-05-20' + (n - 1) * INTERVAL '3 months')::date,
       (DATE '2026-05-20' + (n - 1) * INTERVAL '3 months' + INTERVAL '1 month')::date,
       7500.00,
       0.00,
       'PENDING'
FROM generate_series(1, 40) AS n;

-- keep the ID counters ahead of the IDs used above
SELECT setval(pg_get_serial_sequence('customer_policy', 'policy_id'),
              (SELECT MAX(policy_id) FROM customer_policy));
SELECT setval(pg_get_serial_sequence('premium_installment', 'installment_id'),
              (SELECT MAX(installment_id) FROM premium_installment));
