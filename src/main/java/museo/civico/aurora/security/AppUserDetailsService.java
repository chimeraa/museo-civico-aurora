package museo.civico.aurora.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import museo.civico.aurora.repository.AppUserRepository;

// Cerca l'utente quando fa login, usando la sua email.
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	public AppUserDetailsService(AppUserRepository users) {
		this.users = users;
	}

	// Se l'email non esiste, il login fallisce.
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username == null) {
			throw new UsernameNotFoundException("Credenziali non valide.");
		}
		String email = username.trim().toLowerCase(Locale.ROOT);
		return users.findByEmailIgnoreCase(email)
				.map(AppUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Credenziali non valide."));
	}
}
