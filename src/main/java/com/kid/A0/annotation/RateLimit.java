package com.kid.A0.annotation;

import java.lang.annotation.*;

@Target(value = {ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
}
