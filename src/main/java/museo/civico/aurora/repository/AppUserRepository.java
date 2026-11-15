package museo.civico.aurora.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import museo.civico.aurora.domain.AppUser;

// Accesso al database per gli utenti.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
