![Banner](./banner.png)

# Tau

> Type-checking the everlasting flame at v0.1.0

Tau (τ) is a runtime type-validation library for [GraalVM Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) values. It
was primarily designed for [Fuse](https://github.com/fusemc-dev)'s needs of seamless type-safe integration with JavaScript. Despite its
origins, Tau is a _general-purpose_ library and doesn't rely on Fuse in any way.

## Getting Started

> You'll surely find the artifacts to depend on here once Tau leaves its beta stage.

## Templates

> [!NOTE]
> Throughout this section, the `/.../` circumfix syntax is used to denote `Value` literals. `/42/` thus defines a `Value` of `42`. You will shortly discover that
> the `/.../` syntax isn't used coincidentally.

Type validation in Tau is done through [Template](https://fusemc.dev/tau/blob/master/src/main/java/dev/fusemc/tau/Template.java)s. A template may be thought of
as a _reusable_, _**bidirectional**_ type-schema, that is also capable of _describing_ itself.

A template functions as a type-safe mapping from a Polyglot `Value` to some type `T`, **and vice versa**. The former operation
is referred to as `parse`, and the latter as `serialize`. Both operations produce an `Option` which signals
whether the conversion was successful.

> `Tau.lower()`/`Tau.raise()` are aconvenient ways to assert that the respective operation was successful, and throw
> an _absurdly pretty_ exception if it wasn't:
> 
> ```java
> Tau.lower(Tau.STRING, /"Marie"/);
> ```
