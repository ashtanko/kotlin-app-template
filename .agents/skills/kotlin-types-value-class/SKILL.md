---
name: kotlin-types-value-class
description: Use when writing or reviewing Kotlin type declarations to choose @JvmInline value class over data class where appropriate.
---

# Kotlin value class vs data class

## Core principle

Prefer `@JvmInline value class` for single-field types that carry domain meaning. Data classes are for aggregating multiple fields (this project's default for models — see [`../../reference/coding-conventions.md`](../../reference/coding-conventions.md)).

## Review procedure

1. Find single-property wrappers and primitive-heavy APIs.
2. Decide whether the single value is a real domain distinction. If not, keep the primitive or use a typealias.
3. Check whether replacing the type changes equality, serialization, Java interop, or hot-path boxing.
4. Convert only when the domain meaning is clear and the contract changes are acceptable.
5. Re-run the affected tests (`./gradlew test`, see [`../../reference/commands.md`](../../reference/commands.md)).

## Decision flow

| Situation | Prefer |
|---|---|
| Single field + domain-meaningful (`UserId`, `EmailAddress`, `Percentage`) | `@JvmInline value class` |
| Single field + no domain meaning (just grouping) | Type alias or keep the primitive |
| Multiple fields | Data class |
| Needs custom `equals`/`hashCode` beyond the wrapped value | Data class (value classes delegate to the underlying type) |
| Used as a generic type argument or nullable in a proven hot path | Data class or primitive |

```kotlin
// GOOD: domain-meaningful single field
@JvmInline value class UserId(val value: String)
@JvmInline value class EmailAddress(val value: String)
@JvmInline value class Percentage(val value: Float)

// BAD: data class wrapping a single domain field
data class UserId(val value: String)

// BAD: value class with no domain meaning
@JvmInline value class Wrapper(val value: String) // just use the String, or a type alias

// BAD: value class needing custom equality
@JvmInline value class CaseInsensitiveString(val value: String)
// value class equals delegates to String equals, which IS case-sensitive
// Use a data class if you need different equality semantics
```

## Refactor checks

Before replacing an existing wrapper, check the contract that callers observe:

| Check | Action |
|---|---|
| JSON/API format matters | Verify serialization. `@Serializable data class A(val value: String)` encodes as an object; a value class encodes as the wrapped value. |
| Custom equality or hashing is required | Keep a data class. Value-class equality follows the wrapped value. |
| Callers use `copy()` or destructuring | Keep a data class or update callers deliberately. Value classes do not provide data-class conveniences. |
| Java or reflection-heavy boundary | Verify interop. Java callers see the underlying type; generic/`Any` use boxes. |
| Nullable/generic/vararg hot path | Measure before converting; those uses box. |
| Constructor body, `lateinit`, delegated properties, backing fields | Keep a data class or redesign; value classes only store the constructor value. |

## Common mistakes

| Mistake | Fix |
|---|---|
| Data class wrapping a single domain field | Replace with `@JvmInline value class` |
| Value class with no domain meaning (just a wrapper) | Use a type alias or the primitive directly |
| Value class needing custom equality | Use a data class instead |
| Value class as generic type argument in a hot path | Measure boxing cost; keep the primitive/data class if it matters |
| Forgetting `@JvmInline` annotation | Always pair `value class` with `@JvmInline` for single-field classes |

## Red flags during review

- A data class with exactly one property.
- A `String`, `Long`, or `Int` used where different values should not be interchangeable (e.g., `fun transfer(from: String, to: String, amount: Long)`).
- A type alias used for domain distinction where value-class semantics are needed (type aliases are type-erased, no runtime protection).

## When NOT to apply

- The type needs multiple fields → data class.
- The type needs custom `equals`/`hashCode` → data class.
- The type is used heavily as a nullable or generic in performance-critical code → measure autoboxing cost first (see `./gradlew pitest` and JMH-style measurement before committing to a change purely for performance).
- The project does not need the type-safety distinction → a type alias or primitive is sufficient.
- The replacement would silently change JSON, Java, or reflection-based framework behavior.

## Related

- [`kotlin-functions`](../kotlin-functions/SKILL.md) — where construction/parsing for a new value class should live.
- [`../../reference/coding-conventions.md`](../../reference/coding-conventions.md) — this project's data/state modeling conventions.
