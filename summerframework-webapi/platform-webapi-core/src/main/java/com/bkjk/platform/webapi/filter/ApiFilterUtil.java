package com.bkjk.platform.webapi.filter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

public class ApiFilterUtil {

    public static List<AbstractApiFilter> getFilters(ApplicationContext applicationContext, Method method,
        boolean reversed) {

        ApiFilterExclude[] apiFiltersExcludeOnMethod = method.getAnnotationsByType(ApiFilterExclude.class);
        List<ApiFilter> filters = new ArrayList<>();

        ApiFilter[] apiFiltersOnClass = method.getDeclaringClass().getAnnotationsByType(ApiFilter.class);
        if (apiFiltersOnClass.length > 0) {
            for (ApiFilter apiFilter : apiFiltersOnClass) {
                if (Arrays.asList(apiFiltersExcludeOnMethod).stream().noneMatch(f -> f.value() == apiFilter.value()))
                    filters.add(apiFilter);
            }
        }

        ApiFilter[] apiFiltersOnMethod = method.getAnnotationsByType(ApiFilter.class);
        if (apiFiltersOnMethod.length > 0) {
            for (ApiFilter apiFilter : apiFiltersOnMethod) {
                if (Arrays.asList(apiFiltersExcludeOnMethod).stream().noneMatch(f -> f.value() == apiFilter.value()))
                    filters.add(apiFilter);
            }
        }

        Comparator comparator = Comparator.comparing(ApiFilter::order);
        if (reversed)
            comparator = comparator.reversed();
        filters.sort(comparator);
        List<AbstractApiFilter> filterInstances =
            filters.stream().map(filter -> applicationContext.getBean(filter.value())).collect(Collectors.toList());
        return filterInstances;
    }
}
