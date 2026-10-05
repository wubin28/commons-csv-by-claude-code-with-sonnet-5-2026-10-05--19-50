# TC-2.2: 必需列全部存在，解析成功

## Input
Header: (auto)
RequiredHeaders: date,amount,currency
CSV:
```
date,amount,currency
2026-01-01,100,USD
```

## Expected
Result: OK
RecordCount: 1
Get: currency=USD
