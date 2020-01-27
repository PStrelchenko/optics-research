# optics-research
Discovering magic abilities of optics concept with Kotlin

Inspired by [Arrow Optics](https://arrow-kt.io/docs/optics) and [Monocle](http://julien-truffaut.github.io/Monocle/)

## Problem

Let's examine example of immutable data types composition with nested hierarchy. 

```kotlin
data class Street(val number: Int, val name: String)
data class Address(val city: String, val street: Street)
data class Company(val name: String, val address: Address)
data class Employee(val name: String, val company: Company)
```

Assume that we have instance of ``Employee`` and need to update street name of it's company.
We could use data classes generated ``copy`` method:

```kotlin
employee.copy(
    company = employee.company.copy( 
        address = employee.company.address.copy(
            street = employee.company.address.street.copy(
                name = employee.company.address.street.name.capitalize()
            )
        )
    )
)
```

The deeper the hierarchy of types composition we have, the more code is required to modify each
next inner field. 

## Lens

Let's introduce class ``Lens`` that abstracts the concepts of getting and setting 
the portion (called focus) of some structure ``S`` in functional way: 

```kotlin
class Lens<S, F>(
    val get: (S) -> F,
    val set: (S, F) -> S
) {
    fun modify(s: S, update: (F) -> F): S {
        return set(s, get(s).let(update))
    }
}
```

Then we can create ``Lens`` instance for every field used in our path to deeply nested target:

```kotlin
val Employee.Companion.company = Lens(
    get = Employee::company,
    set = { s, f -> s.copy(company = f) }
)

val Company.Companion.address = Lens(
    get = Company::address,
    set = { s, f -> s.copy(addres = f) }
)

val Address.Companion.street = Lens(
    get = Address::street,
    set = { s, f -> s.copy(street = f) }
)

val Street.Companion.name = Lens(
    get = Street::name,
    set = { s, f -> s.copy(name = f) }
)
```

Given types of ``Lens<A, B>`` and ``Lens<B, C>`` we can compose them to ``Lens<A, C>`` with function ``at``. 
So we can modify target field from employee with composed ``Lens<Employee, String>``:

```kotlin
Employee.company
    .at(Company.address) 
    .at(Adress.street)
    .at(Street.name)
    .modify(employee, String::capitalize)
```

## Optional ([OptLens])

Now let's change type ``Employee`` to have the list of companies instead of single company.

```kotlin
/* ... */
data class Employee(val name: String, val companies: List<Company>)
```

And try to create new instance of ``Employee`` by updating the first item in companies list without optics.

```kotlin
employee.copy(
    companies = employee.companies.mapIndexed { index, company -> 
        if (index == 0) {
            company.copy(
                address = company.address.copy(
                    street = company.address.street.copy(
                        name = company.address.street.name.capitalize()
                    )
                )
            )
        } else {
            company
        }
    }
)
```

With composition of ``Lens`` it could be expressed like this:

```kotlin
Employee.companies
    .atPosition(0)
    .at(Company.address) 
    .at(Adress.street)
    .at(Street.name)
    .modify(employee, String::capitalize)
```

## Traversal
*TBD*

## Optics code generation
*TBD*

## Roadmap

- [x] Lens
- [x] ListTraversal
- [x] Optional
- [ ] ~~Prism~~
- [ ] Optics DSL
- [ ] Find solution for generic Traversals (HKT required?)
- [ ] Property tests for optic laws
- [ ] Performance benchmarks