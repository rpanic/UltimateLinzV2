package model

enum class TournamentStatus(val displayName: String) {

    OPEN("Offen"),
    SIGNED_UP("Angemeldet"),
    NOT_SIGNED_UP("Nicht angemeldet"),
    SPOT("Spot bestätigt"),
    NO_SPOT("Kein Spot"),
    OVER("Vorbei"),
    ARCHIVED("Archiviert")

}
