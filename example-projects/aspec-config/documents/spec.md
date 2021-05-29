# Dsn Requirements

`dsn~dsn-covered~1`

This design is successfully covered.

Needs: utest, impl

Covers:

* `arch~arch-covered~1`



`dsn~dsn-uncovered-impl-only-proposed~1`

This design is partly covered. The impl is only status proposed.

Needs: utest, impl

Covers:

* `arch~arch-impl-only-proposed~1`



`dsn~dsn-covered-impl-duplicates~1`

This design is covered but has one impl duplicated.

Needs: utest, impl

Covers:

* `arch~arch-impl-duplicates~1`


`dsn~dsn-uncovered-impl-wrong-version~1`

This design is uncovered. impl links to wrong version.

Needs: utest, impl

Covers:

* `arch~arch-impl-wrong-version~1`


`dsn~dsn-uncovered-no-imp~1`

This design is uncovered. impl missing.

Needs: utest, impl

Covers:

* `arch~arch~arch-no-imp~1`


`dsn~dsn-covered-superflous-itest~1`

This design is covered. superflous itest coverage.

Needs: utest, impl

Covers:

* `arch~arch~arch-superflous-itest~1`



# Impl requirements

`impl~impl-accepted~1`

This impl is valid.

Covers: 

* `dsn~dsn-covered~1`


`impl~impl-wrong-dsn-version~1`

This impl reference a dsn requirement but uses a wrong version.

Covers:

* `dsn~dsn-uncovered-impl-wrong-version~2`


`impl~impl-wrong-status~1`
Status: proposed

This impl has a non approved status.

Covers:

* `dsn~dsn-uncovered-impl-only-proposed~1`


`impl~impl-duplicate1~1`

This impl is duplicate.

Covers:

* `dsn~dsn-covered-impl-duplicates~1`


`impl~impl-duplicate~1`

This impl is duplicate.

Covers:

* `dsn~dsn-covered-impl-duplicates2~1`


`impl~impl-accepted-plus-unexpected~1`

This impl is valid.

Covers:

* `dsn~dsn-covered-superflous-itest~1`


`itest~itest-unexpected-type~1`

An items that is not in the needs list of the dsn.

Covers:

* `dsn~dsn-covered-superflous-itest~1`


# utest Requirement

`utest~utest-valid~1`

A valid utest requirement covering all dsn requirements.

Covers:

* `dsn~dsn-covered~1`
* `dsn~dsn-uncovered-impl-only-proposed~1`
* `dsn~dsn-covered-impl-duplicates~1`
* `dsn~dsn-uncovered-impl-wrong-version~1`
* `dsn~dsn-uncovered-no-imp~1`
* `dsn~dsn-covered-superflous-itest~1`