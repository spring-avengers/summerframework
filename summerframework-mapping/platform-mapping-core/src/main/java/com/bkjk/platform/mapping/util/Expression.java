package com.bkjk.platform.mapping.util;

import java.io.Serializable;
import java.util.function.Function;

public interface Expression<T, R> extends Function<T, R>, Serializable {
}
