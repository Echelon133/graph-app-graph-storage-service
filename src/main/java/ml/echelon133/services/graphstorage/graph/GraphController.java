package ml.echelon133.services.graphstorage.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphs")
public class GraphController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphController.class);


    @GetMapping("/{hash}")
    public ResponseEntity<String> getGraph(@PathVariable String hash) {
        LOGGER.info(String.format("GET graph with %s hash", hash));
        return new ResponseEntity<>(hash, HttpStatus.OK);
    }
}
