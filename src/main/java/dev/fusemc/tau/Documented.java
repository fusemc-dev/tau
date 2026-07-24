package dev.fusemc.tau;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Indicates that the annotated type is **documented**.
/// 
/// ---
/// A documented type provides a dedicated **identifier** to use over the
/// name of the class. It is expected that, in the appropriate context,
/// the documented identifier unambiguously defines the respective type.
///
/// @since 0.1.0
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Documented {

    @NotNull String value();
}
