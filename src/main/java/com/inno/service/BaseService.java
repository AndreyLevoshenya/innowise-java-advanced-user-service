package com.inno.service;

import com.inno.service.dto.SearchingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BaseService<T, R, K> {
    Page<R> findAll(SearchingRequest searchingRequest, Pageable pageable);

    R findById(K id);

    R create(T createRequest);

    R update(K id, T updateRequest);

    void deleteById(K id);

    int updateActiveStatus(Long id, boolean active);
}
