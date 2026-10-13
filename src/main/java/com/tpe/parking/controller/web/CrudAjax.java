package com.tpe.parking.controller.web;

import com.tpe.parking.exception.BusinessException;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers communs aux contrôleurs web CRUD :
 * détection des requêtes AJAX et fabrication des réponses JSON.
 */
public final class CrudAjax {

    private CrudAjax() {
    }

    /** L'entête X-Requested-With ajouté par tous les fetch() du front. */
    public static boolean estAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /** Réponse de succès : {"success":true,"message":"..."}. */
    public static ResponseEntity<Map<String, Object>> succes(String message) {
        return ResponseEntity.ok(corps(true, message));
    }

    /** Erreur métier ou serveur : {"success":false,"message":"..."} + statut approprié. */
    public static ResponseEntity<Map<String, Object>> erreur(Exception exception) {
        return ResponseEntity.status(statut(exception)).body(corps(false, message(exception)));
    }

    /** Erreur métier avec message explicite (statut 409 par défaut). */
    public static ResponseEntity<Map<String, Object>> erreur(String message, HttpStatus statut) {
        return ResponseEntity.status(statut).body(corps(false, message));
    }

    private static Map<String, Object> corps(boolean success, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        return body;
    }

    private static HttpStatus statut(Exception exception) {
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof BusinessException || exception instanceof InvalidOperationException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception instanceof BusinessException
                    ? "Opération impossible"
                    : "Une erreur inattendue s'est produite";
        }
        return message;
    }
}
