package com.khojo.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail(String email);

	Optional<AppUser> findByPhone(String phone);

	Optional<AppUser> findByGoogleSubject(String googleSubject);

	boolean existsByEmail(String email);
}
