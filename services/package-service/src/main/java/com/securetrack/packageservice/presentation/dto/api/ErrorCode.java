package com.securetrack.packageservice.presentation.dto.api;

public enum ErrorCode {

    PKG_001("PKG_001", "Invalid Package Weight",
            "https://securetrack.com/errors/invalid-package-weight",
            "Le poids du colis ne peut pas être négatif ou supérieur à 500 kg."),

    PKG_002("PKG_002", "Description Required",
            "https://securetrack.com/errors/description-required",
            "La description du colis est obligatoire."),

    PKG_404("PKG_404", "Package Not Found",
            "https://securetrack.com/errors/package-not-found",
            "Aucun colis trouvé avec cet identifiant."),

    VALIDATION_ERROR("VALID_001", "Validation Error",
            "https://securetrack.com/errors/validation-error",
            "Les données fournies sont invalides."),

    INTERNAL_SERVER_ERROR("SYS_001", "Internal Server Error",
            "https://securetrack.com/errors/internal-server-error",
            "Une erreur inattendue s'est produite sur le serveur.");

    private final String code;
    private final String title;
    private final String type;
    private final String defaultDetail;

    ErrorCode(String code, String title, String type, String defaultDetail) {
        this.code = code;
        this.title = title;
        this.type = type;
        this.defaultDetail = defaultDetail;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getDefaultDetail() { return defaultDetail; }
}