package io.quarkiverse.panache.audit.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.ValueGenerationType;

@ValueGenerationType(generatedBy = LastModifiedByGenerator.class)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LastModifiedBy {
}
