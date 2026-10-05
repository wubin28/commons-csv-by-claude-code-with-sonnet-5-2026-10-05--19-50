# TC-2.1: 不启用必需列功能，解析行为与现状完全一致（回归）

## Input
Header: (auto)
RequiredHeaders: (none)
CSV:
```
date,amount
2026-01-01,100
```

## Expected
Result: OK
RecordCount: 1
Get: date=2026-01-01
Get: amount=100
