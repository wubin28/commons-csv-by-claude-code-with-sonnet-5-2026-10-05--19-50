# TC-1.2a: requiredHeaders 内部有重复元素

## Input
Header: date,amount,currency
RequiredHeaders: currency,currency

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: RequiredHeaders contains a duplicate name: "currency" in [currency, currency].
