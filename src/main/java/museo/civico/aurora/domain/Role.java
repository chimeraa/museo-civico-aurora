package museo.civico.aurora.domain;

/**
 * Ruoli applicativi. Le richieste pubbliche di registrazione creano sempre
 * {@link #VISITOR}.
 */
public enum Role {
	VISITOR,
	ADMIN
}
