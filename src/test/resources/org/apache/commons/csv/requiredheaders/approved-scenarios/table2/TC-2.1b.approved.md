# TC-2.1b: requiredHeaders 传空数组，等价于未启用

## Input
Header: date,amount
RequiredHeaders: (empty)
CSV:
```
2026-01-01,100
```

## Expected
Result: OK
RecordCount: 1
Get: date=2026-01-01
Note: Header 显式指定为 date,amount（没有调用 setSkipHeaderRecord(true)），所以 CSV 数据里不重复写表头行，否则表头行本身会被当成第一条数据记录，RecordCount 会变成 2。
