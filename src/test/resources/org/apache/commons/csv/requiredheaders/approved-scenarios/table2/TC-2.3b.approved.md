# TC-2.3b: 同时缺多个必需列，消息要列全

## Input
Header: (auto)
RequiredHeaders: date,amount,currency
CSV:
```
date
2026-01-01
```

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: Missing required header name(s): [amount, currency]. Header names found: [date]
