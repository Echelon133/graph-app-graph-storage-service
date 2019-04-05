package ml.echelon133.services.graphstorage.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@ControllerAdvice
public class APIExceptionHandler extends ResponseEntityExceptionHandler {

    static class ErrorMessage {
        private Date timestamp;
        private String message;
        private String path;

        ErrorMessage(String message, String path) {
            this.timestamp = new Date();
            this.message = message;
            this.path = path;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }
    }

    @ExceptionHandler(value = GraphNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleGraphNotFoundException(GraphNotFoundException ex, WebRequest request) {
        ErrorMessage msg = new ErrorMessage(ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(msg, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = JsonProcessingException.class)
    protected ResponseEntity<ErrorMessage> handleJsonProcessingException(JsonProcessingException ex, WebRequest request) {
        ErrorMessage msg = new ErrorMessage(ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ErrorMessage msg;
        try {
            // Try to get the message from the root cause
            msg = new ErrorMessage(ex.getRootCause().getMessage(), request.getDescription(false));
        } catch (NullPointerException e) {
            // If there is no root cause, display the message from ex
            msg = new ErrorMessage(ex.getMessage(), request.getDescription(false));
        }

        return new ResponseEntity<>(msg, status);
    }
}
