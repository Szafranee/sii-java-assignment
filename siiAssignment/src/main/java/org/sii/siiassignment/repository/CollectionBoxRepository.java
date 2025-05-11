package org.sii.siiassignment.repository;

import org.sii.siiassignment.CollectionBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CollectionBoxRepository extends JpaRepository<CollectionBox, UUID> {
}