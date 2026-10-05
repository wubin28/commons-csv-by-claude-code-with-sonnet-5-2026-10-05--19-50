# TC-1.3: 设置了必需列，但完全没有启用表头模式

## Input
Header: (none)
RequiredHeaders: currency

## Expected
Result: FAIL
ExceptionType: IllegalArgumentException
ExceptionMessage: Field requiredHeaders is set but field header is not set
