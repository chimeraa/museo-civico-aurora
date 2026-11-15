package museo.civico.aurora.domain;

// Ruolo dell'utente: visitatore oppure amministratore.
public enum Role {
	// Chi si registra da solo.
	VISITOR,
	// Gestisce eventi, prenotazioni e statistiche.
	ADMIN
}
