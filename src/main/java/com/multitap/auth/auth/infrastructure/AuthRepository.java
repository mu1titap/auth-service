package com.multitap.auth.auth.infrastructure;

import com.multitap.auth.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
}
