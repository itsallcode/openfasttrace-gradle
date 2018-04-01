# Tracing Example
## Example
`req~exampleB~1`

Example requirement

Needs: dsn

## Example2
`dsn~prefixtagname1~1`

Needs: impl

Covers:
* `req~exampleB~1`

## Example3
`dsn~sub1.tagname2~2`

Needs: utest, itest

Covers:
* `req~exampleB~1`
