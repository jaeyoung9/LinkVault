# LinkVault Payment Policy

## Ad-Free Pass

### Pricing
- **7-Day Pass**: Removes all ads for 7 days from the time of purchase
- **30-Day Pass**: Removes all ads for 30 days from the time of purchase

### Refunds
- Full refund available within 24 hours of purchase
- After 24 hours, refunds are considered on a case-by-case basis for technical issues
- Refunded passes immediately stop suppressing ads

### Duplicate Charges
- The system prevents purchasing a new pass while an active pass exists
- Stripe idempotency protects against duplicate webhook processing
- If a duplicate charge occurs, contact support for immediate resolution

## Donations

### One-Time Donations
- Suggested amounts: $5, $10, $25, $50 (custom amount available)
- Refundable within 7 days of transaction

### Recurring Donations
- Suggested monthly amounts: $3, $5, $10 (custom amount available)
- Cancel anytime from the Settings page
- No partial-month refunds; benefits continue through the current billing period
- Stripe retries failed payments up to 3 times over 7 days before cancellation

### Supporter Benefits
- Supporter badge displayed next to username (symbolic only)
- Badge persists even after donation cancellation
- No features are gated behind supporter status

## Failed Payments
- No charge is applied on payment failure
- Pass or donation is not recorded until payment is confirmed via webhook
- Users receive email notification for failed recurring payments

## Contact
For payment issues, use the support channels available in your Settings page.
