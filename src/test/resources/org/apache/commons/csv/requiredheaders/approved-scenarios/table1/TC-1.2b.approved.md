# TC-1.2b: requiredHeaders 内部有空白元素

## Input
Header: date,amount,currency
RequiredHeaders: currency,<BLANK>

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: RequiredHeaders contains a missing or blank name in [currency, ]
