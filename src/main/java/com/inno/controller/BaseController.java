package com.inno.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface BaseController<T, R, K> {

    ResponseEntity<Page<R>> findAll(String search, Pageable pageable);

    ResponseEntity<R> findById(K id);

    ResponseEntity<R> create(T createRequest);

    ResponseEntity<R> update(Long id, T updateRequest);

    ResponseEntity<Void> deleteById(K id);

    ResponseEntity<Void> updateActiveStatus(Long id, boolean active);
}