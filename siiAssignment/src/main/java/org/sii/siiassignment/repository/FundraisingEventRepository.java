package org.sii.siiassignment.repository;

import org.sii.siiassignment.FundraisingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FundraisingEventRepository extends JpaRepository<FundraisingEvent, UUID> {
}