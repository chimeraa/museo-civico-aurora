package museo.civico.aurora.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import museo.civico.aurora.domain.AppUser;

// Accesso al database per gli utenti.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	// Cerca un utente per email, senza distinguere maiuscole e minuscole.
	Optional<AppUser> findByEmailIgnoreCase(String email);

	// Dice se esiste già un utente con quella email.
	boolean existsByEmailIgnoreCase(String email);
}
