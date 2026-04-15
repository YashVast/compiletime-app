package com.compiletime.infrastructure;

import com.compiletime.domain.BuildSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildSessionRepository extends JpaRepository<BuildSession, Long> {
}
