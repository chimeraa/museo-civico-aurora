package museo.civico.aurora.domain;

// Ruolo dell'utente: visitatore oppure amministratore.
public enum Role {
	// Chi si registra da solo.
	VISITOR,
	// Gli admin gestiscono eventi, prenotazioni e statistiche.
	ADMIN
}
