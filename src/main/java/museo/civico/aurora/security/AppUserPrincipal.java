package museo.civico.aurora.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Role;

/**
 * Principal applicativo: contiene i dati di sessione necessari ai controller e
 * al success handler JSON. Non viene mai serializzato come entity.
 */
public class AppUserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String displayName;
	private final String passwordHash;
	private final Role role;

	public AppUserPrincipal(Long id, String email, String displayName, String passwordHash, Role role) {
		this.id = id;
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public static AppUserPrincipal from(AppUser user) {
		return new AppUserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
				user.getRole());
	}

	public Long getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
