package museo.civico.aurora.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import museo.civico.aurora.repository.AppUserRepository;

// Cerca l'utente al login partendo dall'email, che mette in minuscolo come fa la
// registrazione.
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	public AppUserDetailsService(AppUserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username == null) {
			throw new UsernameNotFoundException("Credenziali non valide.");
		}
		// Stessa pulizia dell'email fatta in registrazione, altrimenti il login non trova l'utente.
		String email = username.trim().toLowerCase(Locale.ROOT);
		return users.findByEmailIgnoreCase(email)
				.map(AppUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Credenziali non valide."));
	}
}
