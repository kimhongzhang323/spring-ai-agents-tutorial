# Consumer Lending Underwriting Policy (v3.4)

## §1 Credit Score Bands
- §1.1 Prime: FICO >= 720. Standard rates apply.
- §1.2 Near-prime: FICO 680–719. Rate premium 0.50%.
- §1.3 Sub-prime: FICO 620–679. Rate premium 1.75%, manual review required.
- §1.4 High-risk: FICO < 620. Auto-decline except documented compensating factors (liquid reserves > 12 months PITI OR LTV < 50%).

## §2 Debt-to-Income (DTI)
- §2.1 Max DTI (back-end) for HOME_PURCHASE: 43%.
- §2.2 Max DTI for AUTO/PERSONAL: 50%.
- §2.3 DTI 36–43% requires secondary review.

## §3 Income Verification
- §3.1 W-2 full-time employees: 2 years consecutive employment OR 1 year + documented industry experience.
- §3.2 Self-employed / contract: 2 years tax returns required; use lower of 2-year average or current year.
- §3.3 Any income discrepancy >10% between stated and verified = BLOCKER pending manual review.

## §4 Fraud & Identity
- §4.1 Fraud score >= 75 = auto-decline.
- §4.2 Fraud score 50–74 = manual review (REFER_TO_HUMAN).
- §4.3 Address state mismatch with employer state requires identity re-verification if loan > $100,000.

## §5 Loan Amount & Term
- §5.1 HOME_PURCHASE: term 120–360 months. Max loan 4x verified annual income.
- §5.2 AUTO: term 24–84 months.
- §5.3 PERSONAL: term 6–60 months, max $50,000.

## §6 Fair Lending Compliance
- §6.1 Decision rationale must reference only the objective factors listed above. Protected-class attributes (age, gender, marital status, national origin) MUST NOT appear in rationale.
- §6.2 Every decline must cite at least one specific §-clause.
- §6.3 Adverse-action notice required for all DECLINED outcomes within 30 days.

## §7 Rate Matrix
- §7.1 Base rate: 6.50% APR for HOME_PURCHASE prime-tier.
- §7.2 Add 0.25% per 20-point FICO drop below 720.
- §7.3 Add 0.50% for self-employed or contract income.
