package com.tpe.parking.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String entity;

    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " avec id=" + id + " non trouvé");
        this.entity = entity;
    }

    public ResourceNotFoundException(String entity, String critere, String valeur) {
        super(entity + " avec " + critere + "=" + valeur + " non trouvé");
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }
}
