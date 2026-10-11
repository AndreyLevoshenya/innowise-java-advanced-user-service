package com.inno.filter;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class EntitySpecification {
    private static final String PERCENTAGE_SYMBOL = "%";

    private static <T> Specification<T> searchByField(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty() || field == null || field.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get(field)), PERCENTAGE_SYMBOL + value.toLowerCase() + PERCENTAGE_SYMBOL);
        };
    }

    public static <T> Specification<T> searchByFields(List<String> fields, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty() || fields == null || fields.isEmpty()) {
                return cb.conjunction();
            }

            Specification<T> spec = null;
            for (String field : fields) {
                Specification<T> fieldSpec = searchByField(field, value);
                spec = (spec == null) ? fieldSpec : spec.or(fieldSpec);
            }
            return spec.toPredicate(root, query, cb);
        };
    }
}